package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.BusinessMatching
import models.renewal.{AMLSTurnover, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.amls_turnover

@Singleton
class AMLSTurnoverController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector,
                                        val renewalService: RenewalService
                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        dataCacheConnector.fetchAll map {
          optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {

              val form = (for {
                renewal <- cache.getEntry[Renewal](Renewal.key)
                turnover <- renewal.turnover
              } yield Form2[AMLSTurnover](turnover)) getOrElse EmptyForm

              Ok(amls_turnover(form, edit, businessMatching.activities))

            }) getOrElse Ok(amls_turnover(EmptyForm, edit, None))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
        Form2[AMLSTurnover](request.body) match {
          case f: InvalidForm =>
            for {
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            } yield BadRequest(amls_turnover(f, edit, businessMatching.activities))
          case ValidForm(_, data) =>
            for {
              renewal <- renewalService.getRenewal
              _ <- renewalService.updateRenewal(renewal.turnover(data))
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.CustomersOutsideUKController.get())
            }
        }
      }
  }

}
