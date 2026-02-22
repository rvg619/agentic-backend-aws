import React, { useState, useEffect } from 'react';
import { Plus, Search, Play, Clock, CheckCircle, AlertCircle } from 'lucide-react';

interface Task {
  id: string;
  title: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface CreateTaskForm {
  title: string;
  description: string;
}

const TaskManager: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [createForm, setCreateForm] = useState<CreateTaskForm>({
    title: '',
    description: ''
  });
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      const response = await fetch('http://localhost:8080/tasks');
      const tasksData = await response.json();
      setTasks(tasksData);
    } catch (error) {
      console.error('Failed to fetch tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.title.trim()) return;

    setIsCreating(true);
    try {
      const response = await fetch('http://localhost:8080/tasks', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          title: createForm.title,
          description: createForm.description
        }),
      });

      if (response.ok) {
        const newTask = await response.json();
        setTasks([newTask, ...tasks]);
        setCreateForm({ title: '', description: '' });
        setShowCreateForm(false);
      }
    } catch (error) {
      console.error('Failed to create task:', error);
    } finally {
      setIsCreating(false);
    }
  };

  const handleRunTask = async (taskId: string) => {
    try {
      const response = await fetch('http://localhost:8080/runs', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          taskId: taskId,
          status: 'PENDING'
        }),
      });

      if (response.ok) {
        alert('Task run started successfully!');
      } else {
        // Handle different error types from the backend
        const errorData = await response.json();
        
        if (errorData.type === 'aws_credentials_missing') {
          alert(`⚠️ AWS Configuration Error:\n\n${errorData.message}\n\nPlease configure your AWS credentials to use Bedrock AI services.`);
        } else {
          alert(`Failed to start task run: ${errorData.message || 'Unknown error'}`);
        }
      }
    } catch (error) {
      console.error('Failed to start task run:', error);
      alert('Failed to start task run: Network error or server unavailable');
    }
  };

  const filteredTasks = tasks.filter(task =>
    task.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    task.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1 className="dashboard-title">Task Manager</h1>
        <p className="dashboard-subtitle">
          Create and manage AI tasks for your agentic system
        </p>
      </div>

      {/* Action Bar */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <div className="card-content">
          <div className="flex justify-between items-center gap-4">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="text"
                placeholder="Search tasks..."
                className="form-input pl-10"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <button
              className="btn btn-primary"
              onClick={() => setShowCreateForm(!showCreateForm)}
            >
              <Plus size={16} />
              Create Task
            </button>
          </div>
        </div>
      </div>

      {/* Create Task Form */}
      {showCreateForm && (
        <div className="card" style={{ marginBottom: '2rem' }}>
          <div className="card-header">
            <h3 className="card-title">Create New Task</h3>
          </div>
          <div className="card-content">
            <form onSubmit={handleCreateTask}>
              <div className="form-group">
                <label className="form-label">Task Title</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Enter a descriptive title for your task"
                  value={createForm.title}
                  onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  className="form-textarea"
                  placeholder="Describe what you want the AI agents to accomplish"
                  rows={4}
                  value={createForm.description}
                  onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                />
              </div>
              <div className="flex gap-3">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isCreating || !createForm.title.trim()}
                >
                  {isCreating ? 'Creating...' : 'Create Task'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowCreateForm(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Tasks List */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Tasks ({filteredTasks.length})</h3>
        </div>
        <div className="card-content">
          {filteredTasks.length === 0 ? (
            <div className="text-center py-8">
              <AlertCircle size={48} className="mx-auto text-gray-400 mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                {searchTerm ? 'No tasks match your search' : 'No tasks yet'}
              </h3>
              <p className="text-gray-500 mb-4">
                {searchTerm 
                  ? 'Try adjusting your search terms' 
                  : 'Create your first task to get started with the agentic system'
                }
              </p>
              {!searchTerm && (
                <button
                  className="btn btn-primary"
                  onClick={() => setShowCreateForm(true)}
                >
                  <Plus size={16} />
                  Create First Task
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Description</th>
                    <th>Created</th>
                    <th>Updated</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTasks.map((task) => (
                    <tr key={task.id}>
                      <td>
                        <div className="font-semibold text-gray-900">{task.title}</div>
                      </td>
                      <td>
                        <div className="text-gray-600 max-w-md truncate">
                          {task.description || 'No description'}
                        </div>
                      </td>
                      <td>
                        <div className="text-sm text-gray-500">
                          {formatDate(task.createdAt)}
                        </div>
                      </td>
                      <td>
                        <div className="text-sm text-gray-500">
                          {formatDate(task.updatedAt)}
                        </div>
                      </td>
                      <td>
                        <div className="flex gap-2">
                          <button
                            className="btn btn-primary btn-sm"
                            onClick={() => handleRunTask(task.id)}
                            title="Run this task"
                          >
                            <Play size={14} />
                            Run
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default TaskManager;