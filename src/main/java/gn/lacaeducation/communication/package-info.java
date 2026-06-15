@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "common",
        "students",
        "attendance::events",
        "grading::events",
        "payments", "payments::events",
        "academics",
        "platform"
    }
)
package gn.lacaeducation.communication;
