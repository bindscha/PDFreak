package models

import scala.util.matching.Regex
import scala.util.{ Try, Success, Failure }
import scala.util.control.Exception

import org.joda.time.DateTime

import scala.slick.driver.ExtendedProfile
import scala.slick.lifted._

import TypeMapper._

// TODO: improve type safety of IDs using different case classes (e.g., UserId(id: Long), etc.)
// don't do it now because it's a source of problems, but when the code base becomes stable enough

/**
 * Database `Profile` for Data Access Layer
 */
trait Profile {
  val profile: ExtendedProfile
}

/**
 * Data Access Layer component for templates
 */
trait TemplateFileComponent {
  self: Profile =>
  import profile.simple._

  /**
   * TemplateFiles table
   *
   * Stores [[models.TemplateFile]] items, which correspond to files in the application
   */
  object TemplateFiles extends Table[TemplateFile]("TEMPLATE") {
    // Columns
    def id = column[String]("ID")
    def content = column[Array[Byte]]("CONTENT")
    def date = column[DateTime]("DATE")
    def description = column[Option[String]]("DESCRIPTION")
    def version = column[Option[String]]("VERSION")

    // Default projection
    def * = id ~ content ~ date ~ description ~ version <> (TemplateFile, TemplateFile.unapply _)

    // Constraints
    def pk = primaryKey("PK", (id, date))
    def idIndex = index("INDEX_ID", id, unique = false)
    def dateIndex = index("INDEX_DATE", id, unique = false)

    // Query templates
    // ...
  }

  /**
   * @return template file for the given identifier
   */
  def templateFile(id: String)(implicit session: Session): Option[TemplateFile] =
    Query(TemplateFiles).where(_.id === id).sortBy(_.date.desc).firstOption

  /**
   * @return template file for the given identifier
   */
  def templateFile(id: String, date: DateTime)(implicit session: Session): Option[TemplateFile] =
    Query(TemplateFiles).where(f => f.id === id && f.date === date).firstOption

  /**
   * @return all template files
   */
  def templateFiles(id: String)(implicit session: Session): Set[TemplateFile] =
    Query(TemplateFiles).where(_.id === id).to[Set]

  /**
   * @return all template files
   */
  def templateFiles(implicit session: Session): Set[TemplateFile] =
    Query(TemplateFiles).to[Set]

  /**
   * Add a new template file
   *
   * @return inserted template file or exception
   */
  def templateFileNew(file: TemplateFile)(implicit session: Session): Try[TemplateFile] =
    Try {
      TemplateFiles.insert(file)
      file
    }

  /**
   * Delete a template file
   *
   * @return deleted template file
   */
  def templateFileDel(id: String)(implicit session: Session): Set[TemplateFile] =
    for {
      ret <- templateFiles(id)
    } yield {
      Exception.allCatch { TemplateFiles.where(f => f.id === id).delete }
      ret
    }

  /**
   * Delete a template file
   *
   * @return deleted template file
   */
  def templateFileDel(id: String, date: DateTime)(implicit session: Session): Option[TemplateFile] =
    for {
      ret <- templateFile(id, date)
    } yield {
      Exception.allCatch { TemplateFiles.where(f => f.id === id && f.date === date).delete }
      ret
    }

}

/**
 * Data Access Layer component for files
 */
trait DatabaseBackedFileComponent {
  self: Profile with TemplateFileComponent =>
  import profile.simple._

  /**
   * DatabaseBackedFiles table
   *
   * Stores [[models.DatabaseBackedFile]] items, which correspond to files in the application
   */
  object DatabaseBackedFiles extends Table[DatabaseBackedFile]("_FILE_") {
    // Columns
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def templateId = column[String]("TEMPLATE_ID")
    def templateDate = column[DateTime]("TEMPLATE_DATE")
    def content = column[Array[Byte]]("CONTENT")
    def name = column[String]("NAME")
    def date = column[DateTime]("DATE")

    // Default projection
    def * = id.? ~ templateId ~ templateDate ~ content ~ name ~ date <> (DatabaseBackedFile, DatabaseBackedFile.unapply _)

    // Insert projection
    def forInsert = templateId ~ templateDate ~ content ~ name ~ date <>
      ((templateId, templateDate, content, name, date) => DatabaseBackedFile(None, templateId, templateDate, content, name, date),
        (f: DatabaseBackedFile) => Some((f.templateId, f.templateDate, f.content, f.name, f.date))) returning id

    // Constraints
    def templateFiles = foreignKey("TEMPLATE_ID_DATE_FK", (templateId, templateDate), TemplateFiles)((t => (t.id, t.date)), ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    // Query templates
    // ...
  }

  /**
   * @return file for the given identifier
   */
  def file(id: Long)(implicit session: Session): Option[DatabaseBackedFile] =
    Query(DatabaseBackedFiles).where(_.id === id).sortBy(_.date.desc).firstOption

  /**
   * @return all files
   */
  def files(implicit session: Session): Set[DatabaseBackedFile] =
    Query(DatabaseBackedFiles).to[Set]

  /**
   * Add a new file
   *
   * @return inserted file or exception
   */
  def fileNew(file: DatabaseBackedFile)(implicit session: Session): Try[DatabaseBackedFile] =
    Try {
      val id = DatabaseBackedFiles.forInsert.insert(file)
      file.copy(id = Some(id))
    }

  /**
   * Delete a file
   *
   * @return deleted file
   */
  def fileDel(id: Long)(implicit session: Session): Option[DatabaseBackedFile] =
    for {
      ret <- file(id)
    } yield {
      Exception.allCatch { DatabaseBackedFiles.where(f => f.id === id).delete }
      ret
    }

}

/**
 * Data Access Layer
 */
class DAL(override val profile: ExtendedProfile)
  extends Profile
  with DatabaseBackedFileComponent
  with TemplateFileComponent
