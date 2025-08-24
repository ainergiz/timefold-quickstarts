package org.acme.employeescheduling.rest;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;

import org.acme.employeescheduling.data.HospitalDataParser;
import org.acme.employeescheduling.domain.HospitalSchedule;
import org.acme.employeescheduling.rest.exception.ErrorInfo;
import org.acme.employeescheduling.rest.exception.EmployeeScheduleSolverException;
import org.acme.employeescheduling.config.ConstraintConfigurationService;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;

@Path("/hospital-schedule")
public class HospitalScheduleResource {

    @Inject
    SolverManager<HospitalSchedule, UUID> solverManager;

    @Inject
    HospitalDataParser hospitalDataParser;

    @Inject
    ConstraintConfigurationService constraintConfigService;

    @POST
    @Path("/solve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response solve() {
        UUID problemId = UUID.randomUUID();
        
        // Load the real hospital data
        HospitalSchedule problem = hospitalDataParser.parseFromResource("/dahiliye.postman.txt");
        
        // Start solving
        SolverJob<HospitalSchedule, UUID> solverJob = solverManager.solve(problemId, problem);
        
        return Response.accepted()
                .entity(new SolvingResponse(problemId.toString(), "Solving started"))
                .build();
    }

    @POST
    @Path("/solve-and-wait")
    @Consumes(MediaType.APPLICATION_JSON) 
    @Produces(MediaType.APPLICATION_JSON)
    public HospitalSchedule solveAndWait() {
        UUID problemId = UUID.randomUUID();
        
        // Load the real hospital data
        HospitalSchedule problem = hospitalDataParser.parseFromResource("/dahiliye.postman.txt");
        
        try {
            // Solve and wait for completion (with 30 second timeout)
            SolverJob<HospitalSchedule, UUID> solverJob = solverManager.solve(problemId, problem);
            return solverJob.getFinalBestSolution();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmployeeScheduleSolverException(problemId.toString(), e);
        }
    }

    @GET
    @Path("/result/{problemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public HospitalSchedule getSchedule(@PathParam("problemId") String problemIdStr) {
        UUID problemId = UUID.fromString(problemIdStr);
        // For simplicity, just solve and return - in production you'd track ongoing jobs
        HospitalSchedule problem = hospitalDataParser.parseFromResource("/dahiliye.postman.txt");
        try {
            SolverJob<HospitalSchedule, UUID> solverJob = solverManager.solve(problemId, problem);
            return solverJob.getFinalBestSolution();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmployeeScheduleSolverException(problemId.toString(), e);
        }
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public SolverStatus getSolverStatus() {
        return new SolverStatus("READY", "Solver is ready to solve problems");
    }

    @GET
    @Path("/test-data")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testData() {
        try {
            HospitalSchedule problem = hospitalDataParser.parseFromResource("/dahiliye.postman.txt");
            return Response.ok(new TestDataResponse(
                problem.getEmployeeCount(),
                problem.getShiftCount(),
                problem.getWorkGroups() != null ? problem.getWorkGroups().size() : 0,
                problem.getWorkplaces() != null ? problem.getWorkplaces().size() : 0
            )).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse("Data parsing failed: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/schedule-view")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getScheduleView() {
        UUID problemId = UUID.randomUUID();
        
        try {
            // Load the problem
            HospitalSchedule problem = hospitalDataParser.parseFromResource("/dahiliye.postman.txt");
            
            // Set default constraint weights for zero-config operation
            var defaultWeights = ConstraintWeightOverrides.of(java.util.Map.ofEntries(
                // Basic scheduling constraints
                java.util.Map.entry("Overlapping shift", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(100)),
                java.util.Map.entry("Insufficient rest after night shift", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(50)),
                java.util.Map.entry("Consecutive night shifts", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(10)),
                java.util.Map.entry("Work on official leave day", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(100)),
                java.util.Map.entry("Work on excluded day", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(20)),
                java.util.Map.entry("Last day shift employees need proper rest", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(30)),
                
                // Individual assignment quota constraints  
                java.util.Map.entry("Max night shifts per employee exceeded", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(80)),
                java.util.Map.entry("Max weekend shifts per employee exceeded", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(60)),
                
                // Workplace qualification constraints
                java.util.Map.entry("Employee group not qualified for workplace", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(200)),
                
                // Work group daily limits (Rule 7)
                java.util.Map.entry("Work group daily limit exceeded", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard(150)),
                
                // NYDH quota distribution constraints (soft - for optimal distribution)
                java.util.Map.entry("NYDH quota limits exceeded", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofSoft(50)),
                
                // Preferences
                java.util.Map.entry("Prefer selected days", ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofSoft(5))
            ));
            problem.setConstraintWeightOverrides(defaultWeights);
            
            // Solve the problem
            SolverJob<HospitalSchedule, UUID> solverJob = solverManager.solve(problemId, problem);
            HospitalSchedule solution = solverJob.getFinalBestSolution();
            
            // Create a readable schedule view
            return Response.ok(createScheduleView(solution)).build();
            
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse("Schedule generation failed: " + e.getMessage()))
                    .build();
        }
    }
    
    private ScheduleView createScheduleView(HospitalSchedule solution) {
        var scheduleView = new ScheduleView();
        scheduleView.score = solution.getScore() != null ? solution.getScore().toString() : "No score";
        scheduleView.employeeCount = solution.getEmployeeCount();
        scheduleView.shiftCount = solution.getShiftCount();
        
        // Group shifts by day and employee
        var shiftsByDay = new java.util.TreeMap<Integer, java.util.List<ShiftAssignment>>();
        
        if (solution.getShifts() != null) {
            for (var shift : solution.getShifts()) {
                int day = shift.getDay();
                shiftsByDay.computeIfAbsent(day, k -> new java.util.ArrayList<>()).add(
                    new ShiftAssignment(
                        shift.getEmployee() != null ? shift.getEmployee().getMemberId() : "UNASSIGNED",
                        shift.getShiftType().toString(),
                        shift.getWorkplace(),
                        shift.getRequiredWorkGroup(),
                        shift.getId()
                    )
                );
            }
        }
        
        scheduleView.shiftsByDay = shiftsByDay;
        return scheduleView;
    }
    
    public static class ScheduleView {
        public String score;
        public int employeeCount;
        public int shiftCount;
        public java.util.Map<Integer, java.util.List<ShiftAssignment>> shiftsByDay;
    }
    
    public static class ShiftAssignment {
        public String employeeName;
        public String shiftType;
        public String workplace;
        public String workGroup;
        public String shiftId;
        
        public ShiftAssignment(String employeeName, String shiftType, String workplace, String workGroup, String shiftId) {
            this.employeeName = employeeName;
            this.shiftType = shiftType;
            this.workplace = workplace;
            this.workGroup = workGroup;
            this.shiftId = shiftId;
        }
    }

    public static class TestDataResponse {
        public int employeeCount;
        public int shiftCount;
        public int workGroupCount;
        public int workplaceCount;

        public TestDataResponse(int employeeCount, int shiftCount, int workGroupCount, int workplaceCount) {
            this.employeeCount = employeeCount;
            this.shiftCount = shiftCount;
            this.workGroupCount = workGroupCount;
            this.workplaceCount = workplaceCount;
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    @DELETE
    @Path("/result/{problemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response terminateSolving(@PathParam("problemId") String problemIdStr) {
        UUID problemId = UUID.fromString(problemIdStr);
        solverManager.terminateEarly(problemId);
        return Response.ok(new SolvingResponse(problemId.toString(), "Solving terminated")).build();
    }

    // Response DTOs
    public static class SolvingResponse {
        public String problemId;
        public String message;

        public SolvingResponse(String problemId, String message) {
            this.problemId = problemId;
            this.message = message;
        }
    }

    public static class SolverStatus {
        public String status;
        public String message;

        public SolverStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    // === CONSTRAINT CONFIGURATION ENDPOINTS ===

    @GET
    @Path("/constraints")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConstraintConfigurations() {
        try {
            return Response.ok(constraintConfigService.getAllConfigurations()).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to get constraint configurations: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/constraints/active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveConstraints() {
        try {
            return Response.ok(constraintConfigService.getActiveConstraintsSummary()).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to get active constraints: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/constraints/{constraintId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConstraint(@PathParam("constraintId") String constraintId, 
                                   ConstraintConfigurationService.ConstraintConfig config) {
        try {
            constraintConfigService.updateConstraintConfig(constraintId, config);
            return Response.ok(new UpdateResponse("Constraint updated: " + constraintId)).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to update constraint: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/constraints/reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetConstraints() {
        try {
            constraintConfigService.resetToDefaults();
            return Response.ok(new UpdateResponse("All constraints reset to defaults")).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(new ErrorResponse("Failed to reset constraints: " + e.getMessage()))
                    .build();
        }
    }

    public static class UpdateResponse {
        public String message;

        public UpdateResponse(String message) {
            this.message = message;
        }
    }
}
