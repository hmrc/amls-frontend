package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.renewal.BusinessTurnover
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.business_turnover

@Singleton
class BusinessTurnoverController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector,
                                        val renewalService: RenewalService
                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      renewalService.getRenewal map {
        response =>
          val form: Form2[BusinessTurnover] = (for {
            renewal <- response
            businessTurnover <- renewal.businessTurnover
          } yield Form2[BusinessTurnover](businessTurnover)).getOrElse(EmptyForm)
          Ok(business_turnover(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }
}
