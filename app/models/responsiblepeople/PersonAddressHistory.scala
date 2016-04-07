package models.responsiblepeople

import play.api.libs.json.Json

case class PersonAddressHistory(personAddress: PersonAddress,
                                personHistory: PersonHistory)

object PersonAddressHistory {

  implicit val format = Json.format[PersonAddressHistory]

}

