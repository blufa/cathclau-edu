@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "common",
        "students", "students::events",
        "attendance", "attendance::events",
        "billing", "billing::events",
        "payments", "payments::events",
        "grading::events",
        "academics",
        "iam",
        "platform"
    }
)
package gn.lacaeducation.notification;
