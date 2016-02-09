package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businessmatching.BusinessMatching
import models.estateagentbusiness.EstateAgentBusiness

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key) map {
        case Some(data) => Ok(views.html.unauthorised(request))
        case _ => Redirect(routes.SummaryController.get())
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
