package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.tradingpremises.routes
import forms.{Form2, _}
import models.DateOfChange
import models.aboutthebusiness.AboutTheBusiness
import models.status.SubmissionDecisionApproved
import models.tradingpremises._
import org.joda.time.LocalDate
import services.StatusService
import utils.{FeatureToggle, RepeatingSection}
import views.html.include.date_of_change

import scala.concurrent.Future


 trait AgentNameController extends RepeatingSection with BaseController {

    val dataCacheConnector: DataCacheConnector
   val statusService: StatusService

    def get(index: Int, edit: Boolean = false) = Authorised.async {
      implicit authContext => implicit request =>

        getData[TradingPremises](index) map {

          case Some(tp) => {
            val form = tp.agentName match {
              case Some(data) => Form2[AgentName](data)
              case None => EmptyForm
            }
            Ok(views.html.tradingpremises.agent_name(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }
    }

   def post(index: Int ,edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AgentName](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.agent_name(f, index,edit)))
        case ValidForm(_, data) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            result <- updateDataStrict[TradingPremises](index) { tp =>
                TradingPremises(tp.registeringAgentPremises,tp.yourTradingPremises,
                  tp.businessStructure, Some(data), None, None, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices, true, tp.lineId, tp.status, tp.endDate)
            }
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved if redirectToDateOfChange(tradingPremises.get, data) =>
              Redirect(routes.AgentNameController.dateOfChange())
            case _ => edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }

   def dateOfChange = FeatureToggle(ApplicationConfig.release7) {
     Authorised {
       implicit authContext => implicit request =>
         Ok(views.html.include.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)), "summary.tradingpremises", routes.AgentNameController.saveDateOfChange()))
     }
   }

   def saveDateOfChange = Authorised.async {
     implicit authContext =>
       implicit request =>
         dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) flatMap { aboutTheBusiness =>
           val extraFields: Map[String, Seq[String]] = aboutTheBusiness.get.activityStartDate match {
             case Some(date) => Map("activityStartDate" -> Seq(date.startDate.toString("yyyy-MM-dd")))
             case None => Map()
           }
           Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
             case form: InvalidForm =>
               Future.successful(BadRequest(date_of_change(form, "summary.tradingpremises", routes.AgentNameController.saveDateOfChange())))
             case ValidForm(_, dateOfChange) =>
               for {
                 tradingPremises <- dataCacheConnector.fetch[TradingPremises](TradingPremises.key)
                 _ <- dataCacheConnector.save[TradingPremises](TradingPremises.key,
                   tradingPremises.agentName(tradingPremises.agentName.get.copy(dateOfChange = Some(dateOfChange))))
               } yield Redirect(routes.SummaryController.get())
           }
         }
   }

   private def redirectToDateOfChange(tradingPremises: TradingPremises, name: AgentName) = {
     ApplicationConfig.release7 && !tradingPremises.agentName.contains(name)
   }
}

object AgentNameController extends AgentNameController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
