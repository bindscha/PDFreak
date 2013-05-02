package controllers

import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Promise }
import ExecutionContext.Implicits.global
import scala.util._
import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import actor._
import models._
import java.io.ByteArrayOutputStream

object Apiv1
    extends Controller
    with DB
    with JsonFormat {

  val RESPONSE_TIMEOUT = 60 seconds
  implicit val timeout = Timeout(RESPONSE_TIMEOUT)

  val reportRouter = Akka.system.actorFor(Akka.system.child("report-router"))
  val resizeRouter = Akka.system.actorFor(Akka.system.child("resize-router"))

  def template(id: String) =
    Action { request =>
      withSession { implicit session =>
        Ok(Json.toJson(DAL.templateFiles(id) map (_.info)))
      }
    }

  def templates =
    Action { request =>
      withSession { implicit session =>
        Ok(Json.toJson(DAL.templateFiles map (_.info)))
      }
    }

  def uploadTemplate(id: String, date: Option[Long] = None, description: Option[String] = None, version: Option[String] = None) =
    Action(parse.multipartFormData) { request =>
      val dataParts = request.body.dataParts
      val fileId = if (id.toLowerCase.endsWith(".docx")) id.substring(0, id.length - 5) else id

      val fileDate = ((for {
        dates <- dataParts get "date"
        dateString <- dates.headOption
        date <- Try { dateString.toLong } toOption
      } yield date) orElse date) map { d => new DateTime(d) }
      val fileDescription = (for {
        descriptions <- dataParts get "description"
        description <- descriptions.headOption
      } yield description) orElse description
      val fileVersion = (for {
        versions <- dataParts get "version"
        version <- versions.headOption
      } yield version) orElse version

      request.body.file("file").map { content =>
        val fileContent = IOUtils.toByteArray(new java.io.FileInputStream(content.ref.file))

        withSession { implicit session =>
          DAL.templateFileNew(TemplateFile(fileId, fileContent, fileDate getOrElse DateTime.now, fileDescription, fileVersion)) match {
            case Success(f) =>
              play.api.Logger.info("Uploaded document " + fileId)
              Ok
            case Failure(e) =>
              play.api.Logger.info("Problem occurred uploading document " + fileId + " -> " + e)
              BadRequest
          }
        }
      } getOrElse {
        BadRequest
      }
    }

  def report(id: Long) =
    Action { request =>
      withSession { implicit session =>
        DAL.fileDel(id) map { f => // TODO: implement download-count-based file persistence
          val in = new java.io.ByteArrayInputStream(f.content)

          Ok.stream(
            Enumerator.fromStream(in) andThen Enumerator.eof
          ).withHeaders(("Content-Disposition", s"attachment; filename=${f.name}"))
        } getOrElse {
          BadRequest
        }
      }
    }

  def reports =
    Action { request =>
      withSession { implicit session =>
        Ok(Json.toJson(DAL.files map (_.info)))
      }
    }

  def generateReport(templateId: String, ext: String, date: Option[Long] = None, inline: Option[String] = None) =
    Action(parse.json) { request =>
      val fileFormat = if (ext.toLowerCase == "docx") "docx" else "pdf"
      val fileName = s"$templateId.$fileFormat"
      withSession { implicit session =>
        (date map (d => DAL.templateFile(templateId, new DateTime(d))) getOrElse DAL.templateFile(templateId)) map { f =>
          val in = new java.io.ByteArrayInputStream(f.content)

          inline match {
            case Some(value) if value.toLowerCase == true || value == "1" =>
              Ok.stream(
                Enumerator outputStream {
                  out => reportRouter ! actor.GenerateReport(templateId, fileFormat, request.body, in, out)
                } andThen Enumerator.eof
              ).withHeaders(
                ("Content-Disposition", s"attachment; filename=$fileName"), 
                ("Access-Control-Allow-Origin", "*"))
            case _ =>
              val enum = Enumerator outputStream {
                out => reportRouter ! actor.GenerateReport(templateId, fileFormat, request.body, in, out)
              }
              val consume = Iteratee.consume[Array[Byte]]()

              Async {
                Iteratee.flatten(enum(consume)).run map { content =>
                  withSession { implicit session =>
                    DAL.fileNew(DatabaseBackedFile(None, f.id, f.date, content, fileName)) match {
                      case Success(f) if f.id.isDefined =>
                        play.api.Logger.info("Generated report id " + templateId + " and stored result as file id " + f.id.get)
                        Ok(Json.toJson(f.info)).withHeaders(("Access-Control-Allow-Origin", "*"))
                      case _ =>
                        play.api.Logger.info("Generated report id " + templateId + " but failed to store result")
                        InternalServerError
                    }
                  }
                }
              }
          }
        } getOrElse {
          BadRequest
        }
      }
    }

  def generateReportPreflight(templateId: String, ext: String, date: Option[Long] = None, inline: Option[String] = None) =
    Action { request =>
      Ok.withHeaders(
        ("Access-Control-Allow-Origin", "*"),
        ("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept"))
    }

  def resizeDocument(id: String, size: Option[String] = None, scale: Option[Float] = None) =
    Action(parse.multipartFormData) { request =>
      val dataParts = request.body.dataParts
      val fileSize = PageSize.fromString((size orElse (dataParts get "size").flatMap(_.headOption)) getOrElse "A4")
      val scaleInData = (dataParts get "scale").flatMap(_.headOption).flatMap(s => Try(s.toDouble.floatValue).toOption)
      val fileScale: Float = (scale orElse scaleInData) getOrElse 1f

      request.body.file("file").map { content =>
        val fileInputStream = new java.io.FileInputStream(content.ref.file)
        Ok.stream(
          Enumerator outputStream {
            out => resizeRouter ! actor.ResizeDocument(id, fileSize, fileScale, fileInputStream, out)
          } andThen Enumerator.eof
        ).withHeaders(("Content-Disposition", s"attachment; filename=$id.pdf"))
      } getOrElse {
        BadRequest
      }
    }

}
