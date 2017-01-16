package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.aboutthebusiness.AboutTheBusiness
import models.hvd.Hvd
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RepeatingSection
import views.html.date_of_change

import scala.concurrent.Future

trait HvdDateOfChangeController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get = Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(date_of_change(EmptyForm, "summary.hvd", routes.HvdDateOfChangeController.post())))
  }

  def compareAndUpdateDate(hvd: Hvd, newDate: DateOfChange): Hvd = {
    hvd.dateOfChange match {
      case Some(s4ltrDate) => s4ltrDate.dateOfChange.isAfter(newDate.dateOfChange) match {
        case true => hvd
        case false => hvd.dateOfChange(newDate)
      }
      case _ => hvd
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
    getModelWithDateMap() flatMap {
      case (hvd, startDate) =>
      Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDate) match {
        case f: InvalidForm =>
      Future.successful(BadRequest(date_of_change(f, "summary.hvd", routes.HvdDateOfChangeController.post())))
        case ValidForm(_, data) => {
          for {
          _ <- dataCacheConnector.save[Hvd](Hvd.key, compareAndUpdateDate(hvd , data))
          } yield {
            Redirect(routes.SummaryController.get())
          }
        }
      }
    }
  }

  private def getModelWithDateMap()(implicit authContext: AuthContext, hc: HeaderCarrier): Future[(Hvd, Map[_ <: String, Seq[String]])] = {
    dataCacheConnector.fetchAll map {
      optionalCache =>
        (for {
          cache <- optionalCache
          aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
          hvd <- cache.getEntry[Hvd](Hvd.key)
        } yield (hvd, aboutTheBusiness.activityStartDate)) match {
          case Some((hvd, Some(activityStartDate))) => (hvd, Map("activityStartDate" -> Seq(activityStartDate.startDate.toString("yyyy-MM-dd"))))
          case Some((hvd, _)) => (hvd, Map())
          case _ =>(Hvd(), Map())
        }
    }
  }
}

object HvdDateOfChangeController extends HvdDateOfChangeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override def dataCacheConnector = DataCacheConnector
}

