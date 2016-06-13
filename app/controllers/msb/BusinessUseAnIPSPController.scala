package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness._
import views.html.msb.business_use_an_ipsp

import scala.concurrent.Future

trait BusinessUseAnIPSPController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[BusinessUseAnIPSP] = (for {
            msb <- response
            businessUseAnIPSP <- msb.businessUseAnIPSP
          } yield Form2[BusinessUseAnIPSP](businessUseAnIPSP)).getOrElse(EmptyForm)
          Ok(business_use_an_ipsp(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[BusinessUseAnIPSP](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(business_use_an_ipsp(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.businessUseAnIPSP(data))

          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())

          }
      }
    }
  }
}

object BusinessUseAnIPSPController extends BusinessUseAnIPSPController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
