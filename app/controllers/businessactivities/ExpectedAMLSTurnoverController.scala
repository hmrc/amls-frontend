package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.ExpectedAMLSTurnover
import models.businessactivities.{BusinessActivities, _}
import views.html.businessactivities._
import models.businessmatching.{HighValueDealing, MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices, _}
import play.api.i18n.Messages

import scala.concurrent.Future

trait ExpectedAMLSTurnoverController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
          optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            mlrActivities <- businessMatching.activities
          } yield {
            (for {
              businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              expectedTurnover <- businessActivities.expectedAMLSTurnover
            } yield Ok(expected_amls_turnover(Form2[ExpectedAMLSTurnover](expectedTurnover), edit, businessTypes(businessMatching))))
              .getOrElse (Ok(expected_amls_turnover(EmptyForm, edit, businessTypes(businessMatching))))
          }) getOrElse Ok(expected_amls_turnover(EmptyForm, edit, None))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedAMLSTurnover](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key).map {
            businessMatching => dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
              BadRequest(expected_amls_turnover(f, edit, businessTypes(businessMatching.getOrElse(None))))
          }
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.expectedAMLSTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
      }
    }
  }

  private def businessTypes(activities: BusinessMatching): Option[String] = {
    val typesString = activities.activities map { a =>
      a.businessActivities.map { line =>
        line match {
          case AccountancyServices => Messages("businessmatching.registerservices.servicename.lbl.01")
          case BillPaymentServices => Messages("businessmatching.registerservices.servicename.lbl.02")
          case EstateAgentBusinessService => Messages("businessmatching.registerservices.servicename.lbl.03")
          case HighValueDealing => Messages("businessmatching.registerservices.servicename.lbl.04")
          case MoneyServiceBusiness => Messages("businessmatching.registerservices.servicename.lbl.05")
          case TrustAndCompanyServices => Messages("businessmatching.registerservices.servicename.lbl.06")
          case TelephonePaymentService => Messages("businessmatching.registerservices.servicename.lbl.07")
        }
      }
    }

    typesString match{
      case Some(types) => Some(typesString.getOrElse(Set()).mkString(", ") + ".")
      case None => None
    }

  }
}

object ExpectedAMLSTurnoverController extends ExpectedAMLSTurnoverController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
