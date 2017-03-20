package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import models.registrationprogress.{NotStarted, Section, Started}
import models.status.{Complete, Incomplete}
import play.api.i18n.MessagesApi
import play.api.mvc.Call
import services.ProgressService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()
(
  val authConnector: AuthConnector,
  dataCacheConnector: DataCacheConnector,
  progressService: ProgressService,
  messages: MessagesApi
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>

        val renewalSection = Section(messages("renewal.section.title"), Started, false, Call("test", "test"))

        val block = for {
          cache <- OptionT(dataCacheConnector.fetchAll)
        } yield {
          val variationSections = progressService.sections(cache)

          Ok(renewal_progress(renewalSection, variationSections, canSubmit = false))
        }

        block getOrElseF Future.successful(Ok(renewal_progress(renewalSection, Seq.empty, canSubmit = false)))

  }

}
