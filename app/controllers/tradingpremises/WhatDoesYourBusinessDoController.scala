package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.businessmatching._
import models.status.SubmissionDecisionApproved
import models.tradingpremises.{MsbServices, TradingPremises, WhatDoesYourBusinessDo}
import org.joda.time.LocalDate
import play.api.mvc.Result
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{DateOfChangeHelper, RepeatingSection}
import views.html.tradingpremises._

import scala.concurrent.Future

trait WhatDoesYourBusinessDoController extends RepeatingSection with BaseController with FormHelpers with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  private def data(index: Int, edit: Boolean)(implicit ac: AuthContext, hc: HeaderCarrier)
  : Future[Either[Result, (CacheMap, Set[BusinessActivity])]] = {
    dataCacheConnector.fetchAll map {
      cache =>
        (for {
          c: CacheMap <- cache
          bm <- c.getEntry[BusinessMatching](BusinessMatching.key)
          activities <- bm.activities flatMap {
            _.businessActivities match {
              case set if set.isEmpty => None
              case set => Some(set)
            }
          }
        } yield (c, activities))
          .fold[Either[Result, (CacheMap, Set[BusinessActivity])]] {
          // TODO: Need to think about what we should do in case of this error
          Left(Redirect(routes.WhereAreTradingPremisesController.get(index, edit)))
        } {
          t => Right(t)
        }
    }
  }

  // scalastyle:off cyclomatic.complexity
  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      data(index, edit) flatMap {
        case Right((c, activities)) =>
          if (activities.size == 1) {
            // If there is only one activity in the data from the pre-reg,
            // then save that and redirect immediately without showing the
            // 'what does your business do' page.
            updateDataStrict[TradingPremises](index) { tp =>
                Some(tp.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activities)))
            }
            Future.successful {
              activities.contains(MoneyServiceBusiness) match {
                case true => Redirect(routes.MSBServicesController.get(index))
                case false => Redirect(routes.PremisesRegisteredController.get(index))
              }
            }
          } else {
            val ba = BusinessActivities(activities)
            Future.successful {
              getData[TradingPremises](c, index) match {
                case Some(TradingPremises(_,_, _, _,_,_,Some(wdbd),_,_,_,_,_)) =>
                  Ok(what_does_your_business_do(Form2[WhatDoesYourBusinessDo](wdbd), ba, edit, index))
                case Some(TradingPremises(_,_,  _,_,_,_, None, _,_,_,_,_)) =>
                  Ok(what_does_your_business_do(EmptyForm, ba, edit, index))
                case _ => NotFound(notFoundView)
              }
            }
          }
        case Left(result) => Future.successful(result)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      data(index, edit) flatMap {
        case Right((c, activities)) =>
          Form2[WhatDoesYourBusinessDo](request.body) match {
            case f: InvalidForm =>
              val ba = BusinessActivities(activities)
              Future.successful {
                BadRequest(what_does_your_business_do(f, ba, edit, index))
              }
            case ValidForm(_, data) => {
              for {
                tradingPremises <- getData[TradingPremises](index)
                _ <- updateDataStrict[TradingPremises](index) {
                  case tp if data.activities.contains(MoneyServiceBusiness) =>
                    tp.whatDoesYourBusinessDoAtThisAddress(data)
                  case tp if !data.activities.contains(MoneyServiceBusiness) =>
                    TradingPremises(
                      tp.registeringAgentPremises,
                      tp.yourTradingPremises,
                      tp.businessStructure,
                      tp.agentName,
                      tp.agentCompanyName,
                      tp.agentPartnership,
                      Some(data),
                      None,
                      true,
                      tp.lineId,
                      tp.status,
                      tp.endDate
                    )
                }
                status <- statusService.getStatus
              } yield status match {
                case SubmissionDecisionApproved if !data.activities.contains(MoneyServiceBusiness) && redirectToDateOfChange(tradingPremises, data) && edit =>
                  Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
                case _ => data.activities.contains(MoneyServiceBusiness) match {
                  case true => Redirect(routes.MSBServicesController.get(index, edit))
                  case _ => edit match {
                    case true => Redirect (routes.SummaryController.getIndividual (index) )
                    case false => Redirect (routes.PremisesRegisteredController.get (index) )
                  }
                }
              }
            }.recoverWith{
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
        case Left(result) => Future.successful(result)
      }
  }

  def dateOfChange(index: Int) = Authorised {
    implicit authContext => implicit request =>
      Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)), "summary.tradingpremises", routes.WhatDoesYourBusinessDoController.saveDateOfChange(index)))
  }

  def saveDateOfChange(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[TradingPremises](index) flatMap { tradingPremises =>
          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDateFormFields(tradingPremises.startDate)) match {
            case form: InvalidForm =>
              Future.successful(BadRequest(views.html.date_of_change(
                form.withMessageFor(DateOfChange.errorPath, tradingPremises.startDateValidationMessage),
                "summary.tradingpremises", routes.WhatDoesYourBusinessDoController.saveDateOfChange(index))))
            case ValidForm(_, dateOfChange) =>
              for {
                _ <- updateDataStrict[TradingPremises](index) { tradingPremises =>
                  tradingPremises.whatDoesYourBusinessDoAtThisAddress(tradingPremises.whatDoesYourBusinessDoAtThisAddress.get.copy(dateOfChange = Some(dateOfChange)))
                }
              } yield Redirect(routes.SummaryController.get())
          }
        }
  }

  def redirectToDateOfChange(tradingPremises: Option[TradingPremises], model: WhatDoesYourBusinessDo) =
    ApplicationConfig.release7 && tradingPremises.lineId.isDefined && !tradingPremises.get.whatDoesYourBusinessDoAtThisAddress.contains(WhatDoesYourBusinessDo)

  // scalastyle:on cyclomatic.complexity
}

object WhatDoesYourBusinessDoController extends WhatDoesYourBusinessDoController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
