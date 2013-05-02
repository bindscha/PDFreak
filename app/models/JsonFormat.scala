package models

// I'm using the selfless trait pattern here...
// I believe the increased flexibility should be worth any possible performance
// impact, but let's revisit this at some point and make sure.
trait JsonFormat {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val templateFileInfoFormat: Format[TemplateFileInfo] = Json.format[TemplateFileInfo]

  implicit val databaseBackedFileInfoFormat: Format[DatabaseBackedFileInfo] = Json.format[DatabaseBackedFileInfo]

}

object JsonFormat extends JsonFormat
