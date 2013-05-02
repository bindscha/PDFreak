package config

object Config {

  import play.api.Play.current
  import play.api.Play.configuration

  import scala.concurrent.duration._

  val DB_DEFAULT_DRIVER =
    configuration.getString("db.default.driver") getOrElse "org.h2.Driver"

  val APP_BASE_URL =
    configuration.getString("app.baseurl") getOrElse "//localhost"

}