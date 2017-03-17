package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import services.ProgressService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()(val authConnector: AuthConnector, dataCacheConnector: DataCacheConnector, progressService: ProgressService) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>

      val block = for {
        cache <- OptionT(dataCacheConnector.fetchAll)
      } yield  {
        val variationSections = progressService.sections(cache)

        Ok(renewal_progress(variationSections, canSubmit = false))
      }

      block getOrElseF Future.successful(Ok(renewal_progress(Seq.empty, canSubmit = false)))

  }

}
