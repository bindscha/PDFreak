package actor

import play.api.libs.concurrent._
import play.api.libs.json._
import play.api.Play.current

import akka.actor._
import akka.routing._

import scala.collection.mutable.{ Map => MMap, Seq => MSeq }
import collection.JavaConversions._

import java.io._

import com.itextpdf.text._
import com.itextpdf.text.pdf._

object ResizeProcessor {

  def start() = {
    Akka.system.actorOf(Props[ResizeProcessorActor].withRouter(FromConfig()), "resize-router")
  }

}

sealed trait PageSize
case object A4 extends PageSize
case object A5 extends PageSize

object PageSize {
  def toString(size: PageSize) =
    size match {
      case A4 => "A4"
      case A5 => "A5"
    }
  def fromString(str: String) =
    str.toUpperCase match {
      case "A5" => A5
      case _ => A4
    }
  def toIText(size: PageSize) =
    size match {
      case A4 => com.itextpdf.text.PageSize.A4
      case A5 => com.itextpdf.text.PageSize.A5
    }
}

case class ResizeDocument(id: String, size: PageSize, scale: Float, in: InputStream, out: OutputStream)

class ResizeProcessorActor extends Actor with ActorLogging {

  override def receive = {
    case ResizeDocument(id, size, scale, in, out) =>
      val reader = new PdfReader(in)
      val doc = new Document(PageSize.toIText(size), 0, 0, 0, 0)
      val writer = PdfWriter.getInstance(doc, out)
      doc.open()
      val cb = writer.getDirectContent()
      for (i <- 1 until reader.getNumberOfPages + 1) {
        doc.newPage()
        val page = writer.getImportedPage(reader, i)
        cb.addTemplate(page, scale, 0, 0, scale, 0, 0)
      }
      doc.close()
      log.info("Document '" + id + "' resized to " + PageSize.toString(size) + " (" + (scale * 100).toInt + "%)")
  }

}
