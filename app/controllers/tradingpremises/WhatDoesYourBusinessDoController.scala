package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import play.api.mvc.Result
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RepeatingSection
import views.html.tradingpremises._

import scala.concurrent.Future

trait WhatDoesYourBusinessDoController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

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
            updateDataStrict[TradingPremises](index) {
              case Some(tp) =>
                Some(tp.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activities)))
              case _ =>
                Some(TradingPremises(
                  whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(activities))
                ))
            }
            Future.successful {
              activities.contains(MoneyServiceBusiness) match {
                case true => Redirect(routes.MSBServicesController.get(index))
                case false => Redirect(routes.SummaryController.get())
              }
            }
          } else {
            val ba = BusinessActivities(activities)
            Future.successful {
              getData[TradingPremises](c, index) match {
                case Some(TradingPremises(_,_, _, _,_,Some(wdbd),_)) =>
                  Ok(what_does_your_business_do(Form2[WhatDoesYourBusinessDo](wdbd), ba, edit, index))
                case Some(TradingPremises(_,_,  _,_,_, None, _)) =>
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
              updateDataStrict[TradingPremises](index) {
                case Some(tp) if data.activities.contains(MoneyServiceBusiness) =>
                  Some(tp.whatDoesYourBusinessDoAtThisAddress(data))
                case Some(tp) if !data.activities.contains(MoneyServiceBusiness) =>
                  Some(TradingPremises(None,tp.yourTradingPremises, tp.yourAgent,None, None,Some(data), None))
                case _ => Some(TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(data)))
              } map {
                _ => data.activities.contains(MoneyServiceBusiness) match {
                  case true => Redirect(routes.MSBServicesController.get(index, edit))
                  case false => edit match {
                    case true => Redirect(routes.SummaryController.getIndividual(index))
                    case false => Redirect(routes.SummaryController.get())
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

  // scalastyle:on cyclomatic.complexity
}

object WhatDoesYourBusinessDoController extends WhatDoesYourBusinessDoController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
