import play.api._
import play.api.mvc._
import play.api.Play.current

/**
 * <code>Global</code> contains application-global settings
 *
 * <br />
 *
 * @author Laurent Bindschaedler <b>laurent.bindschaedler@epfl.ch</b>
 * @maintainer Laurent Bindschaedler <b>laurent.bindschaedler@epfl.ch</b>
 * @version 0.5
 */
object Global extends GlobalSettings {

  override def onStart(application: Application) {

    Logger.info("Starting report processing backend...")
    actor.ReportProcessor.start()
    Logger.info("Done")

    Logger.info("Starting convert processing backend...")
    actor.ConvertProcessor.start()
    Logger.info("Done")

    Logger.info("Starting resize processing backend...")
    actor.ResizeProcessor.start()
    Logger.info("Done")

  }

}
