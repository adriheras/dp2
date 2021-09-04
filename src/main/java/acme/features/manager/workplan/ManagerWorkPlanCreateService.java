
package acme.features.manager.workplan;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.components.SpamFilterService;
import acme.entities.tasks.Task;
import acme.entities.workPlans.WorkPlan;
import acme.features.manager.ManagerRepository;
import acme.features.manager.task.ManagerTaskRepository;
import acme.framework.components.Errors;
import acme.framework.components.Model;
import acme.framework.components.Request;
import acme.framework.entities.Manager;
import acme.framework.services.AbstractCreateService;

@Service
public class ManagerWorkPlanCreateService implements AbstractCreateService<Manager, WorkPlan> {

	@Autowired
	private ManagerWorkPlanRepository	repository;
	@Autowired
	private ManagerRepository			managerRepo;

	@Autowired
	private ManagerTaskRepository		managerTaskRepo;

	@Autowired
	protected SpamFilterService			spamFilterService;


	@Override
	public boolean authorise(final Request<WorkPlan> request) {
		assert request != null;

		return true;
	}

	@Override
	public void bind(final Request<WorkPlan> request, final WorkPlan entity, final Errors errors) {
		assert request != null;
		assert entity != null;
		assert errors != null;

		request.bind(entity, errors);
	}

	@Override
	public void unbind(final Request<WorkPlan> request, final WorkPlan entity, final Model model) {
		assert request != null;
		assert entity != null;
		assert model != null;
		final List<Task> tasks;
		final Manager manager = this.managerRepo.findOne(request.getPrincipal().getActiveRoleId());
		tasks = this.managerTaskRepo.findByOwnerAndNotStarted(manager.getId(), new Date()).stream().collect(Collectors.toList());

		model.setAttribute("allTasks", tasks);
		request.unbind(entity, model, "title", "tasks", "executionStart", "executionEnd", "isPrivate", "tasksParsed");

	}

	@Override
	public WorkPlan instantiate(final Request<WorkPlan> request) {
		assert request != null;

		final WorkPlan result;
		final Manager manager;

		manager = this.managerRepo.findOne(request.getPrincipal().getActiveRoleId());

		result = new WorkPlan();
		result.setOwner(manager);
		result.setTasks(new ArrayList<Task>());
		result.setOwner(manager);

		return result;
	}

	@Override
	public void validate(final Request<WorkPlan> request, final WorkPlan entity, final Errors errors) {
		assert request != null;
		assert entity != null;
		assert errors != null;
		final Manager manager = this.managerRepo.findOne(request.getPrincipal().getActiveRoleId());

		if (!errors.hasErrors("executionStart")) {
			if (entity.getExecutionStart() != null && entity.getExecutionEnd() != null) {
				// executionStart must be in the future
				final Date now = new Date();
				errors.state(request, now.before(entity.getExecutionStart()), "executionStart", "manager.work-plan.form.error.start");
			} else {
				if (entity.getExecutionStart() == null) {
					errors.state(request, true, "executionStart", "manager.task.form.error.start");
				}
				if (entity.getExecutionEnd() == null) {
					errors.state(request, true, "executionEnd", "manager.task.form.error.end");
				}
			}
		}
		if (!errors.hasErrors("executionEnd")) {
			if (entity.getExecutionStart() != null && entity.getExecutionEnd() != null) {
				// executionEnd must be after executionStart
				errors.state(request, entity.getExecutionEnd().after(entity.getExecutionStart()), "executionEnd", "manager.work-plan.form.error.end");
			} else {
				if (entity.getExecutionStart() == null) {
					errors.state(request, true, "executionStart", "manager.task.form.error.start");
				}
				if (entity.getExecutionEnd() == null) {
					errors.state(request, true, "executionEnd", "manager.task.form.error.end");
				}
			}
		}
		//tasks not set yet
		//ASSUME: tasks were already created and were tested for spam
		final Boolean isSpam = this.spamFilterService.isSpam(entity.getTitle());
		errors.state(request, !isSpam, "*", "manager.work-plan.form.error.spamDetected");

		final List<Task> tasks = new ArrayList<>();
		final String tasksParsed = entity.getTasksParsed();
		final String[] tasksId = tasksParsed.split(",");
		if (!tasksParsed.isEmpty()) {
			for (int i = 0; i < tasksId.length; i++) {
				final Task task = this.managerTaskRepo.findOne(Integer.parseInt(tasksId[i]));
				tasks.add(task);
			}
		}
		if (!errors.hasErrors("tasks") && tasks.isEmpty()) {
			errors.state(request, false, "tasks", "manager.work-plan.form.error.noTasks");
		}
		if (entity.getExecutionStart() != null && entity.getExecutionEnd() != null) {
		// WORKPLAN CAN'T START AFTER THE FIRST TASK HAS STARTED
		if (!errors.hasErrors("tasks") && !errors.hasErrors("executionStart") && !errors.hasErrors("executionEnd")) {
			for (final Task t : tasks) {
				if (t.getExecutionStart().before(entity.getExecutionStart())) {
					errors.state(request, false, "tasks", "manager.work-plan.form.error.executionStartTooLate");
					break;
				}
			}
		}
		// WORKPLAN CAN'T FINISH BEFORE THE LAST TASK HAS FINISHED
		if (!errors.hasErrors("tasks") && !errors.hasErrors("executionStart") && !errors.hasErrors("executionEnd")) {
			for (final Task t : tasks) {
				if (t.getExecutionEnd().after(entity.getExecutionEnd())) {
					errors.state(request, false, "tasks", "manager.work-plan.form.error.executionEndTooEarly");
					break;
				}
			}
		}
		}

		//No private task in public workplan
		boolean priv = false;
		if (Boolean.TRUE.equals(!entity.getIsPrivate())) {
			for (final Task t : tasks) {
				if (Boolean.TRUE.equals(t.getIsPrivate())) {
					priv = true;
				}
			}
		}

		errors.state(request, !priv, "tasks", "manager.work-plan.form.error.private");

		final List<Task> allTasks = this.managerTaskRepo.findByOwnerAndNotStarted(manager.getId(), new Date()).stream().collect(Collectors.toList());
		final Model model = request.getModel();
		model.setAttribute("allTasks", allTasks);
		request.setModel(model);
	}

	@Override
	public void create(final Request<WorkPlan> request, final WorkPlan entity) {
		assert request != null;
		assert entity != null;

		final List<Task> tasks = new ArrayList<>();
		final String tasksParsed = entity.getTasksParsed();
		final String[] tasksId = tasksParsed.split(",");

		if (tasksId.length > 0) {
			for (int i = 0; i < tasksId.length; i++) {
				final Task task = this.managerTaskRepo.findOne(Integer.parseInt(tasksId[i]));
				tasks.add(task);
			}
		}
		entity.setTasks(tasks);
		final String workloadParsed = entity.getWorkloadParsed();
		entity.setWorkloadParsed(workloadParsed);

		final boolean isSpam = this.spamFilterService.isSpam(entity.getTitle(), entity.getTasks().toString());
		if (!isSpam) {
			this.repository.save(entity);
		} else {
			System.out.println("SPAM: " + entity.getTitle() + " " + entity.getTasks().toString());
			System.out.println("Mensaje borrado");
		}
	}

}