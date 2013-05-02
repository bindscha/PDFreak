package models

import org.joda.time.{ DateTime, Interval }
import java.sql.Timestamp

import com.bindscha.scatter.Random
import com.bindscha.scatter.Regex.{ R_EMAIL, R_URL }

import config.Config

case class TemplateFile(
    id: String,
    content: Array[Byte],
    date: DateTime = DateTime.now,
    description: Option[String] = None,
    version: Option[String] = None) {
  require(id != null && id.length > 0, "Invalid template id")
  require(content != null && content.size > 0, "Invalid template content")
  require(date != null && date.isBefore(DateTime.now.plusSeconds(10)), "Invalid template date")
  require(description != null && (description.isEmpty || description.get.length > 0), "Invalid template description")
  require(version != null && (version.isEmpty || version.get.length > 0), "Invalid template version")

  val info = TemplateFileInfo(id, date, description, version)
}

case class TemplateFileInfo(
    id: String,
    date: DateTime = DateTime.now,
    description: Option[String] = None,
    version: Option[String] = None) {
  require(id != null && id.length > 0, "Template id invalid")
  require(date != null && date.isBefore(DateTime.now.plusSeconds(10)), "Invalid template date")
  require(description != null && (!description.isDefined || description.get.length > 0), "Invalid template description")
  require(version != null && (!version.isDefined || version.get.length > 0), "Invalid template version")
}

case class DatabaseBackedFile(
    id: Option[Long],
    templateId: String,
    templateDate: DateTime,
    content: Array[Byte],
    name: String,
    date: DateTime = DateTime.now) {
  require(id != null && (!id.isDefined || id.get > 0), "Invalid database backed file identifier")
  require(templateId != null && templateId.length > 0, "Invalid database backed file template identifier")
  require(templateDate != null && templateDate.isBefore(DateTime.now.plusSeconds(10)), "Invalid database backed file template date")
  require(name != null && name.length > 0, "Invalid database backed file name")
  require(content != null && content.size > 0, "Invalid database backed file content")
  require(date != null && date.isBefore(DateTime.now.plusSeconds(10)), "Invalid database backed file date")

  val info = DatabaseBackedFileInfo(id, templateId, templateDate, name, Config.APP_BASE_URL + controllers.routes.Apiv1.report(id getOrElse -1L), date)
}

case class DatabaseBackedFileInfo(
    id: Option[Long],
    templateId: String,
    templateDate: DateTime,
    name: String,
    url: String,
    date: DateTime = DateTime.now) {
  require(id != null && (!id.isDefined || id.get > 0), "Invalid database backed file identifier")
  require(templateId != null && templateId.length > 0, "Invalid database backed file template identifier")
  require(templateDate != null && templateDate.isBefore(DateTime.now.plusSeconds(10)), "Invalid database backed file template date")
  require(name != null && name.length > 0, "Invalid database backed file name")
  require(url != null && url.length > 0, "Invalid database backed file url")
  require(date != null && date.isBefore(DateTime.now.plusSeconds(10)), "Invalid database backed file date")
}

