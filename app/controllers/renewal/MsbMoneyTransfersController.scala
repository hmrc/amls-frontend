package controllers.renewal

import javax.inject.Inject

import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class MsbMoneyTransfersController @Inject()(val authConnector: AuthConnector) extends BaseController {
  // $COVERAGE-OFF$
  def get = Authorised.async {
    implicit authContext => implicit request => ???
  }
  // $COVERAGE-ON$
}
