package models.status

sealed trait StatusState

class Current extends StatusState
class Complete extends StatusState
class Incomplete extends StatusState

