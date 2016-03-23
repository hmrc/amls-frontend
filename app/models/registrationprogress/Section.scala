package models.registrationprogress

import play.api.mvc.Call

case class Section(
                  name: String,
                  status: Status,
                  call: Call
                  )
