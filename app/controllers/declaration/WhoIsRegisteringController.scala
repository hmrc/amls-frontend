package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.declaration._
import models.responsiblepeople.{PositionWithinBusiness, ResponsiblePeople}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.declaration.who_is_registering

trait WhoIsRegisteringController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
        optionalCache =>
          (for {
            cache <- optionalCache
            responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
          } yield {
            (for {
              whoIsRegistering <- cache.getEntry[WhoIsRegistering](WhoIsRegistering.key)
            } yield Ok(who_is_registering(Form2[WhoIsRegistering](whoIsRegistering), responsiblePeople)))
              .getOrElse (Ok(who_is_registering(EmptyForm, responsiblePeople)))
          }) getOrElse Ok(who_is_registering(EmptyForm, Seq.empty))
      }
  }

  def getAddPerson(whoIsRegistering: WhoIsRegistering, responsiblePeople: Seq[ResponsiblePeople]): Option[AddPerson] = {

    val rp = responsiblePeople.filter(_.personName.exists(name=> whoIsRegistering.people.equals(name.firstName.concat(name.lastName)))).head
    rp.personName match {
      case Some(name) => Some(AddPerson(name.firstName, name.middleName, name.lastName,
        rp.positions.fold[Set[PositionWithinBusiness]](Set.empty)(x => x.positions)))
      case _ => None
    }
  }

  implicit def getPosition(positions: Set[PositionWithinBusiness]): RoleWithinBusiness = {
    import models.responsiblepeople._

    positions.head match {
      case BeneficialOwner => BeneficialShareholder
      case Director => models.declaration.Director
      case InternalAccountant => models.declaration.InternalAccountant
      case NominatedOfficer => models.declaration.NominatedOfficer
      case Partner => models.declaration.Partner
      case SoleProprietor => models.declaration.SoleProprietor
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request => {
      Form2[WhoIsRegistering](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
            case Some(data) => BadRequest(who_is_registering(f, data))
            case None => BadRequest(who_is_registering(f, Seq.empty))
          }
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
                whoIsRegistering <- cache.getEntry[WhoIsRegistering](WhoIsRegistering.key)
              } yield {
                dataCacheConnector.save[WhoIsRegistering](WhoIsRegistering.key, data)
                data.people match {
                  case "-1" => {
                    dataCacheConnector.save[AddPerson](AddPerson.key, AddPerson("", None, "", BeneficialShareholder))
                    Redirect(routes.AddPersonController.get())
                  }
                  case _ => {
                    getAddPerson(data, responsiblePeople) map {addPerson =>
                       dataCacheConnector.save[AddPerson](AddPerson.key, addPerson)
                    }
                    Redirect(routes.DeclarationController.get())
                  }
                }
              }) getOrElse Redirect(routes.DeclarationController.get())
          }
      }
    }
  }
}

object WhoIsRegisteringController extends WhoIsRegisteringController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
