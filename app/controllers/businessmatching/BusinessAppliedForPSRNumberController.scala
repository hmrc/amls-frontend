package controllers.businessmatching

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessAppliedForPSRNumber
import views.html.businessmatching.business_applied_for_psr_number

import scala.concurrent.Future

trait BusinessAppliedForPSRNumberController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[BusinessAppliedForPSRNumber] = (for {
            ba <- response
            number <- ba.businessAppliedForPSRNumber
          } yield Form2[BusinessAppliedForPSRNumber](number)).getOrElse(EmptyForm)
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
            ba <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              ba.businessAppliedForPSRNumber(data)
            )
          } yield edit match {
            case _ => Redirect(routes.SummaryController.get())
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
