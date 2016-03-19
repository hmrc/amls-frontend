package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.businessmatching.{CompanyRegistrationNumber, BusinessMatching}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.business_matching_company_registration_number

import scala.concurrent.Future

trait CompanyRegistrationNumberController extends BaseController {

  private[controllers] def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[CompanyRegistrationNumber] = (for {
            businessMatching <- response
            registrationNumber <- businessMatching.companyRegistrationNumber
          } yield Form2[CompanyRegistrationNumber](registrationNumber)).getOrElse(EmptyForm)
          Ok(business_matching_company_registration_number(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CompanyRegistrationNumber](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_matching_company_registration_number(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              businessMatching.companyRegistrationNumber(data)
            )
          } yield edit match {
            case true => Redirect(routes.RegisterServicesController.get())
            //TODO: This will need to point to the business matching summary page.
            case false => Redirect(controllers.routes.MainSummaryController.onPageLoad())
          }
      }
    }
  }


}

object CompanyRegistrationNumberController extends CompanyRegistrationNumberController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] def dataCacheConnector: DataCacheConnector = DataCacheConnector
}