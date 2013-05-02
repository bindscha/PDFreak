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

import org.apache.poi.xwpf.usermodel._
import org.apache.poi.xwpf.converter.pdf._

import models._
import models.DB._

object ConvertProcessor {

  def start() = {
    Akka.system.actorOf(Props[ConvertProcessorActor].withRouter(FromConfig()), "convert-router")
  }

}

case class ConvertDocument(in: InputStream, out: OutputStream)

class ConvertProcessorActor extends Actor with ActorLogging {

  override def receive = {
    case ConvertDocument(in, out) =>
      // 1) Load DOCX into XWPFDocument
      val document = new XWPFDocument(in)

      // 2) Prepare Pdf options
      val options = PdfOptions.create().fontEncoding("windows-1250")

      // 3) Convert XWPFDocument to Pdf
      PdfConverter.getInstance.convert(document, out, options)

      log.info("Converted DOCX to PDF")
  }

}
