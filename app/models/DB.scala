package models

trait DB {

  import play.api.db.{ DB => PlayDB }
  import play.api.Play.current

  import scala.slick.driver._
  import scala.slick.session.{ Database, Session }

  import config.Config

  val DB = Database.forDataSource(PlayDB.getDataSource())

  val DAL = Config.DB_DEFAULT_DRIVER match {
    case "org.h2.Driver" => new DAL(H2Driver)
    case "org.postgresql.Driver" => new DAL(PostgresDriver)
    case _ => sys.error("Database driver must be either org.h2.Driver or org.postgresql.Driver")
  }

  def withSession[A](f: Session => A) = DB withSession { f }

  def withTransaction[A](f: Session => A) = DB withTransaction { f }

}

object DB extends DB
