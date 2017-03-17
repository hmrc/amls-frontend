package controllers.renewal

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector


@Singleton
class InvolvedInOtherController @Inject()(val authConnector: AuthConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    ???
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }
}



