package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.registrationprogress.Completed
import models.status.ReadyForRenewal
import play.api.i18n.MessagesApi
import services.{ProgressService, RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()
(
  val authConnector: AuthConnector,
  dataCacheConnector: DataCacheConnector,
  progressService: ProgressService,
  messages: MessagesApi,
  renewals: RenewalService,
  statusService :StatusService
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        renewals.getSection flatMap { renewalSection =>

          val canSubmit = renewalSection.status == Completed

          val block = for {
            cache <- OptionT(dataCacheConnector.fetchAll)
            statusInfo <- OptionT.liftF(statusService.getDetailedStatus)
          } yield {
            val variationSections = progressService.sections(cache)
            val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
            val msbOrTcspExists = ControllerHelper.isMSBSelected(businessMatching) || ControllerHelper.isTCSPSelected(businessMatching)

            statusInfo match {
              case (ReadyForRenewal(renewalDate), _) => Ok(renewal_progress(renewalSection, variationSections, canSubmit, msbOrTcspExists, renewalDate))
              case _ => throw new Exception("Cannot get renewal date")
            }

          }

          block getOrElse InternalServerError("Cannot get business matching or renewal date")
        }
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>

      Future.successful(Redirect(controllers.declaration.routes.WhoIsRegisteringController.get()))
  }

}
