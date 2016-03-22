package controllers.testonly

import config.{BusinessCustomerSessionCache, AMLSAuthConnector, AmlsShortLivedCache}
import controllers.auth.AmlsRegime
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object TestOnlyController extends TestOnlyController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}

trait TestOnlyController extends FrontendController with Actions {

  def dropSave4Later = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        BusinessCustomerSessionCache.remove()
        AmlsShortLivedCache.remove(user.user.oid).map { x =>
          Ok("Cache successfully cleared")
        }
  }
}
