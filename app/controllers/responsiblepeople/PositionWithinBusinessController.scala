package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.{BusinessType, BusinessMatching}
import models.responsiblepeople._
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.position_within_business

import scala.concurrent.Future

trait PositionWithinBusinessController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromDeclaration:Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetchAll map { optionalCache =>
          (optionalCache map { cache =>
            val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
              .getOrElse(BusinessType.SoleProprietor)

            getData[ResponsiblePeople](cache, index) match {
              case Some(ResponsiblePeople(_, _, _, _, Some(positions), _, _, _, _, _, _, _, _, _))
              => Ok(position_within_business(Form2[Positions](positions), edit, index, bt, fromDeclaration))
              case Some(ResponsiblePeople(_, _, _, _, _, _, _, _, _, _, _, _, _, _))
              => Ok(position_within_business(EmptyForm, edit, index, bt, fromDeclaration))
              case _
              => NotFound(notFoundView)
            }
          }).getOrElse(NotFound(notFoundView))
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      import jto.validation.forms.Rules._
      implicit authContext => implicit request =>
        Form2[Positions](request.body) match {
          case f: InvalidForm =>
            for {
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            } yield {
              BadRequest(position_within_business(f, edit, index,
                ControllerHelper.getBusinessType(businessMatching).getOrElse(BusinessType.SoleProprietor), fromDeclaration))
            }
          case ValidForm(_, data) => {
            def personalTaxRouter = {
              (data.personalTax, edit) match {
                case (false, false) => Redirect(routes.ExperienceTrainingController.get(index, false, fromDeclaration))
                case (false, true) => Redirect(routes.DetailedAnswersController.get(index, false))
                case _ => Redirect(routes.VATRegisteredController.get(index, edit, fromDeclaration))
              }
            }
            for {
              _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                rp.positions(data)
              }
              rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
            } yield {
              if (hasNominatedOfficer(rpSeqOption)) {
                edit match {
                  case true => Redirect(routes.DetailedAnswersController.get(index))
                  case _ => Redirect(routes.VATRegisteredController.get(index, edit))
                }
              } else {
                Redirect(routes.AreTheyNominatedOfficerController.get(index))
              }
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }

  private[controllers] def hasNominatedOfficer(rpSeqOption: Option[Seq[ResponsiblePeople]]): Boolean = {
    rpSeqOption match {

      case Some(rps) => rps.exists {
        rp => rp.positions match {
          case Some(position) => position.isNominatedOfficer
          case _ => false
        }
      }
      case _ => false
    }
  }
}

object PositionWithinBusinessController extends PositionWithinBusinessController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
