@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "common",
        "students",
        "attendance::events",
        "grading::events",
        "billing::events",
        "payments", "payments::events",
        "academics",
        "iam",
        "platform", "platform::events"
    }
)
package gn.lacaeducation.communication;
