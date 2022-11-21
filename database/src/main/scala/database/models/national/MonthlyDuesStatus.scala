package org.dsasf.members
package database.models.national

enum MonthlyDuesStatus(val value: String):
  case Active extends MonthlyDuesStatus("active")
  case Lapsed extends MonthlyDuesStatus("lapsed")
  case PastDue extends MonthlyDuesStatus("past_due")
  case TwoMonthAfterFailed extends MonthlyDuesStatus("2mo_plus_failed")
  case CanceledByAdmin extends MonthlyDuesStatus("canceled_by_admin")
  case CanceledByProcessor extends MonthlyDuesStatus("canceled_by_processor")
  case CanceledByFailure extends MonthlyDuesStatus("canceled_by_failure")
  case Never extends MonthlyDuesStatus("never")
