@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "common",
        "students",
        "staff",
        "attendance::events",
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
