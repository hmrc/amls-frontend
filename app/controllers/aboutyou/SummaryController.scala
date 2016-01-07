package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import models.AboutYou
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait SummaryController extends FrontendController with Actions {

  def dataCache: DataCacheConnector

  def get = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit user => implicit request =>
      dataCache.fetchDataShortLivedCache[AboutYou](AboutYou.key, user.user.oid) map {
        case Some(data) => Ok(views.html.about_you_summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}