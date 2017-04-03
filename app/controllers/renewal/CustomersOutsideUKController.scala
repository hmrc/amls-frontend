package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, HighValueDealing}
import models.renewal.{CustomersOutsideUK, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.renewal._

import scala.concurrent.Future

@Singleton
class CustomersOutsideUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                             val authConnector: AuthConnector,
                                             val renewalService: RenewalService
                                            ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      renewalService.getRenewal map {
        response =>
          val form: Form2[CustomersOutsideUK] = (for {
            renewal <- response
            customers <- renewal.customersOutsideUK
          } yield Form2[CustomersOutsideUK](customers)).getOrElse(EmptyForm)
          Ok(customers_outside_uk(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[CustomersOutsideUK](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(customers_outside_uk(f, edit)))
        case ValidForm(_, data) => {
          dataCacheConnector.fetchAll map { optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              renewal <- cache.getEntry[Renewal](Renewal.key)
            } yield {
              renewalService.updateRenewal(renewal.customersOutsideUK(data))
              redirectDependingOnActivities(businessMatching)
            }) getOrElse Redirect(routes.SummaryController.get())
          }
        }
      }
  }

  private def redirectDependingOnActivities(businessMatching: BusinessMatching) = {
    ControllerHelper.getBusinessActivity(Some(businessMatching)) match {
      case Some(activities) if activities.businessActivities contains HighValueDealing => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
      case _ => Redirect(routes.SummaryController.get())
    }
  }
}


