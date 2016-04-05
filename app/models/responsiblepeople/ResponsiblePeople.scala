package models.responsiblepeople

import typeclasses.MongoKey

case class ResponsiblePeople(addPerson: Option[AddPerson] = None) {

  def addPerson(ap: AddPerson): ResponsiblePeople =
    this.copy(addPerson = Some(ap))

  def saRegistered(sa: SaRegistered): ResponsiblePeople =
    this.copy(saRegistered = Some(sa))
}

object ResponsiblePeople {

  import play.api.libs.json._

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePeople] {
    override def apply(): String = key
  }

  implicit val format = Json.format[ResponsiblePeople]

  implicit def default(responsiblePeople: Option[ResponsiblePeople]): ResponsiblePeople =
    responsiblePeople.getOrElse(ResponsiblePeople())

}