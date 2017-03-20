package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.businessactivities.BusinessActivities
import models.businessmatching._
import models.renewal.InvolvedInOther
import play.api.i18n.Messages
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.involved_in_other

@Singleton
class InvolvedInOtherController @Inject()(
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector,
                                         val statusService: StatusService
                                         ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              } yield {
                (for {
                  businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
                  involvedInOther <- businessActivities.involvedInOther
                } yield {
                  Ok(involved_in_other(Form2[InvolvedInOther](InvolvedInOther.convertToRenewal(involvedInOther)),
                    edit, businessTypes(businessMatching)))
                })
                  .getOrElse(Ok(involved_in_other(EmptyForm, edit, businessTypes(businessMatching))))
              }) getOrElse Ok(involved_in_other(EmptyForm, edit, None))
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

    typesString match {
      case Some(types) => Some(typesString.getOrElse(Set()).mkString(", ") + ".")
      case None => None
    }

  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }
}



