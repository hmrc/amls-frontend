package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.renewal.BusinessTurnover
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

@Singleton
class BusinessTurnoverController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector
                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessTurnover](BusinessTurnover.key) map {
        response =>
          val form: Form2[BusinessTurnover] = (for {
            expectedTurnover <- response
          } yield Form2[BusinessTurnover](expectedTurnover)).getOrElse(EmptyForm)
          Ok(business_turnover(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }
}
