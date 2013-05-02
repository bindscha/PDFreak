package actor

import play.api.libs.concurrent._
import play.api.libs.json._
import play.api.Play.current

import akka.actor._
import akka.routing._

import scala.collection.mutable.{ Map => MMap, Seq => MSeq }
import collection.JavaConversions._

import java.io._

import fr.opensagres.xdocreport.core._
import fr.opensagres.xdocreport.document._
import fr.opensagres.xdocreport.document.registry._
import fr.opensagres.xdocreport.template._
import fr.opensagres.xdocreport.template.formatter._

import models._
import models.DB._

object ReportProcessor {

  def start() = {
    Akka.system.actorOf(Props[ReportProcessorActor].withRouter(FromConfig()), "report-router")
  }

}

case class GenerateReport(id: String, format: String, model: JsValue, template: InputStream, out: OutputStream)

case class Project(Name: String)

class ReportProcessorActor extends Actor with ActorLogging {

  val convertRouter = Akka.system.actorFor(Akka.system.child("convert-router"))

  override def receive = {
    case GenerateReport(id, format, model, template, out) =>
      // 1) Load Docx file by filling Velocity template engine and cache it to the registry
      val report = XDocReportRegistry.getRegistry.loadReport(template, TemplateEngineKind.Velocity)
      metadatify(report, model)

      // 2) Create context Java model
      val context = report.createContext()
      contextify(context, model)

      format match {
        case "docx" =>
          report.process(context, out)
          log.info("Generated report id: " + id + " as DOCX")
        case _ =>
          val outPipe = new PipedOutputStream
          val inPipe = new PipedInputStream(outPipe)

          convertRouter ! ConvertDocument(inPipe, out)

          report.process(context, outPipe)

          log.info("Generated report id: " + id + " as PDF")
      }
  }

  private def metadatify(report: IXDocReport, model: JsValue): Unit = {
    def metadata(value: JsValue, key: String = ""): List[String] =
      value match {
        case o: JsObject =>
          Nil ::: (o.fields.flatMap(f => metadata(f._2, (if (key.length > 0) key + "." + f._1 else f._1)))).toList
        case a: JsArray =>
          if (a.value.forall(_.isInstanceOf[JsObject])) {
            a.value.flatMap(_.asInstanceOf[JsObject].fields map (f => key + "." + f._1)).toSet.toList
          } else {
            Nil
          }
        case _ => Nil
      }

    val fmd = new FieldsMetadata
    metadata(model) foreach fmd.addFieldAsList
    report.setFieldsMetadata(fmd)
  }

  private def contextify(context: IContext, model: JsValue): Unit = {
    def convert(value: JsValue): Any = {
      value match {
        case o: JsObject =>
          mapAsJavaMap(MMap[String, Any]((for ((k, v) <- o.fields) yield { (k, convert(v)) }): _*))
        case a: JsArray =>
          seqAsJavaList(MSeq((for (v <- a.value) yield convert(v)): _*))
        case s: JsString =>
          s.as[String]
        case n: JsNumber =>
          n.value.toString
        case unexpected =>
          sys.error("Unexpected JSON value: " + unexpected)
      }
    }

    convert(model) match {
      case m: java.util.Map[String @unchecked, Any @unchecked] =>
        for ((k, v) <- m) {
          context.put(k, v)
        }
      case unexpected =>
        sys.error("Unexpected conversion result: " + unexpected)
    }
  }

}
