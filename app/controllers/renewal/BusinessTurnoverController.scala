package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.renewal.{Renewal, BusinessTurnover}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.business_turnover

import scala.concurrent.Future

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
    implicit authContext => implicit request => {
      Form2[BusinessTurnover](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](Renewal.key)
            _ <- renewalService.updateRenewal(renewal.businessTurnover(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.AMLSTurnoverController.get())
          }
      }
    }
  }
}
