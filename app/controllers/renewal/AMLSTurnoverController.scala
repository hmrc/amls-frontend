package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.BusinessMatching
import models.renewal.AMLSTurnover
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.amls_turnover

@Singleton
class AMLSTurnoverController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector
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
                turnover <- cache.getEntry[AMLSTurnover](AMLSTurnover.key)
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
              _ <- dataCacheConnector.save[AMLSTurnover](AMLSTurnover.key, data)
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.CustomersOutsideUKController.get())
            }
        }
      }
  }

}
