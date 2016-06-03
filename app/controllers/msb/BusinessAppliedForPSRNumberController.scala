package controllers.msb

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.moneyservicebusiness.{BusinessAppliedForPSRNumber, MoneyServiceBusiness}
import views.html.msb.business_applied_for_psr_number

import scala.concurrent.Future

trait BusinessAppliedForPSRNumberController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[BusinessAppliedForPSRNumber] = (for {
            msb <- response
            businessFranchise <- msb.businessAppliedForPSRNumber
          } yield Form2[BusinessAppliedForPSRNumber](businessFranchise)).getOrElse(EmptyForm)
          Ok(business_applied_for_psr_number(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BusinessAppliedForPSRNumber](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_applied_for_psr_number(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.businessAppliedForPSRNumber(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())
          }
      }
    }
  }
}

object BusinessAppliedForPSRNumberController extends BusinessAppliedForPSRNumberController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
