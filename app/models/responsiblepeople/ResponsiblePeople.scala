package models.responsiblepeople

import typeclasses.MongoKey

case class ResponsiblePeople(addPerson: Option[AddPerson] = None) {

  def addPerson(ap: AddPerson): ResponsiblePeople =
    this.copy(addPerson = Some(ap))
}

object ResponsiblePeople {

  import play.api.libs.json._

  val key = "responsible-people"

  implicit val mongoKey = new MongoKey[ResponsiblePeople] {
    override def apply(): String = "responsible-people"
  }

  implicit val format = Json.format[ResponsiblePeople]

  /*  implicit val reads: Reads[ResponsiblePeople] =  {
  /*
      import play.api.libs.functional.syntax._
      import play.api.libs.json.Reads._
      import play.api.libs.json._
  */
      (
        (__.read[Option[AddPerson]])
      ) (ResponsiblePeople.apply _)
    }

    implicit val writes: Writes[ResponsiblePeople] = Writes[ResponsiblePeople] {
      model =>
        Seq(
          Json.toJson(model.addPerson).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    } */

  implicit def default(responsiblePeople: Option[ResponsiblePeople]): ResponsiblePeople =
    responsiblePeople.getOrElse(ResponsiblePeople())

}