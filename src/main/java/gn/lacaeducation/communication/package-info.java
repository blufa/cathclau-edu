@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "common", "common::pdf",
        "students",
        "staff",
        "audit::events",
        "attendance::events",
        "calendar::events",
        "grading::events",
        "billing::events",
        "payments", "payments::events",
        "payroll::events",
        "academics",
        "iam", "iam::events",
        "platform", "platform::events"
    }
)
package gn.lacaeducation.communication;
