package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.DateOfChange
import models.status.SubmissionDecisionApproved
import models.tradingpremises.{MsbServices, TradingPremises}
import org.joda.time.LocalDate
import play.api.mvc.{Action, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait MSBServicesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(tp) => {
          val form = tp.msbServices match {
            case Some(service) => Form2[MsbServices](service)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.msb_services(form, index, edit))
        }
        case None => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.msb_services(f, index, edit)))
        case ValidForm(_, data) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            _ <- updateDataStrict[TradingPremises](index) { tp =>
              tp.msbServices(data)
            }
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved if redirectToDateOfChange(tradingPremises, data) =>
              Redirect(routes.MSBServicesController.dateOfChange(index))
            case _ => edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.PremisesRegisteredController.get(index))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  def redirectToDateOfChange(tradingPremises: TradingPremises, msbServices: MsbServices) =
    ApplicationConfig.release7 && !tradingPremises.msbServices.contains(msbServices)

  def dateOfChange(index: Int) = Authorised {
    implicit authContext => implicit request =>
      Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)), "summary.tradingpremises", routes.MSBServicesController.saveDateOfChange(index)))
  }

  def saveDateOfChange(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[TradingPremises](index) flatMap { tradingPremises =>
          val extraFields = tradingPremises.yourTradingPremises.fold(Map[String, Seq[String]]()) { ytp =>
            Map("activityStartDate" -> Seq(ytp.startDate.toString("yyyy-MM-dd")))
          }

          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case form: InvalidForm =>
              Future.successful(BadRequest(views.html.date_of_change(form, "summary.tradingpremises", routes.MSBServicesController.saveDateOfChange(index))))
            case ValidForm(_, dateOfChange) =>
              for {
                _ <- dataCacheConnector.save[TradingPremises](TradingPremises.key,
                  tradingPremises.msbServices(tradingPremises.msbServices.get.copy(dateOfChange = Some(dateOfChange))))
              } yield Redirect(routes.SummaryController.get())
          }
        }
  }
}

object MSBServicesController extends MSBServicesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
