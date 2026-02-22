import React, { useState, useEffect } from 'react';
import { RefreshCw, Eye, Clock, CheckCircle, XCircle, AlertTriangle, ChevronDown, ChevronRight } from 'lucide-react';

interface Run {
  id: string;
  taskId: string;
  status: 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED';
  createdAt: string;
  updatedAt: string;
}

interface Step {
  id: string;
  name: string;
  description: string;
  status: string;
  result?: string;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
  artifacts: Artifact[];
}

interface Artifact {
  id: string;
  name: string;
  type: string;
  content: string;
  size: number;
  createdAt: string;
  updatedAt: string;
}

const RunMonitor: React.FC = () => {
  const [runs, setRuns] = useState<Run[]>([]);
  const [selectedRun, setSelectedRun] = useState<string | null>(null);
  const [runSteps, setRunSteps] = useState<Step[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [expandedSteps, setExpandedSteps] = useState<Set<string>>(new Set());

  useEffect(() => {
    fetchRuns();
    // Auto-refresh every 5 seconds
    const interval = setInterval(fetchRuns, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchRuns = async () => {
    try {
      setRefreshing(true);
      const response = await fetch('http://localhost:8080/runs');
      const runsData = await response.json();
      setRuns(runsData.sort((a: Run, b: Run) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      ));
    } catch (error) {
      console.error('Failed to fetch runs:', error);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const fetchRunSteps = async (runId: string) => {
    try {
      const response = await fetch(`http://localhost:8080/runs/${runId}/steps`);
      const stepsData = await response.json();
      setRunSteps(stepsData);
    } catch (error) {
      console.error('Failed to fetch run steps:', error);
      setRunSteps([]);
    }
  };

  const handleRunSelect = async (runId: string) => {
    if (selectedRun === runId) {
      setSelectedRun(null);
      setRunSteps([]);
    } else {
      setSelectedRun(runId);
      await fetchRunSteps(runId);
    }
  };

  const toggleStepExpanded = (stepId: string) => {
    const newExpanded = new Set(expandedSteps);
    if (newExpanded.has(stepId)) {
      newExpanded.delete(stepId);
    } else {
      newExpanded.add(stepId);
    }
    setExpandedSteps(newExpanded);
  };

  const getStatusIcon = (status: string) => {
    switch (status.toUpperCase()) {
      case 'PENDING':
        return <Clock size={16} className="text-yellow-500" />;
      case 'RUNNING':
        return <RefreshCw size={16} className="text-blue-500 animate-spin" />;
      case 'DONE':
      case 'COMPLETED':
        return <CheckCircle size={16} className="text-green-500" />;
      case 'FAILED':
        return <XCircle size={16} className="text-red-500" />;
      default:
        return <AlertTriangle size={16} className="text-gray-500" />;
    }
  };

  const getStatusBadge = (status: string) => {
    const baseClasses = "status-badge";
    switch (status.toUpperCase()) {
      case 'PENDING':
        return `${baseClasses} pending`;
      case 'RUNNING':
        return `${baseClasses} running`;
      case 'DONE':
      case 'COMPLETED':
        return `${baseClasses} done`;
      case 'FAILED':
        return `${baseClasses} failed`;
      default:
        return `${baseClasses} pending`;
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const formatDuration = (startDate: string, endDate?: string) => {
    const start = new Date(startDate);
    const end = endDate ? new Date(endDate) : new Date();
    const diffMs = end.getTime() - start.getTime();
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);

    if (diffHours > 0) return `${diffHours}h ${diffMins % 60}m`;
    if (diffMins > 0) return `${diffMins}m ${diffSecs % 60}s`;
    return `${diffSecs}s`;
  };

  const getArtifactDisplayClass = (type: string) => {
    switch (type) {
      case 'text':
        return 'bg-gray-50 text-gray-800';
      case 'json':
        return 'bg-gray-100 text-green-800';
      default:
        return 'bg-white text-gray-600';
    }
  };

  const formatArtifactContent = (artifact: Artifact) => {
    if (artifact.type === 'json') {
      try {
        return JSON.stringify(JSON.parse(artifact.content), null, 2);
      } catch {
        return artifact.content;
      }
    }
    return artifact.content;
  };

  const showFullArtifact = (artifact: Artifact) => {
    alert(artifact.content);
  };

  const getArtifactActions = (artifact: Artifact) => {
    return (
      <div className="mt-2 flex gap-2">
        <button className="text-xs text-green-600 hover:underline px-2 py-1 bg-green-50 rounded">
          Download
        </button>
        <button className="text-xs text-red-600 hover:underline px-2 py-1 bg-red-50 rounded">
          Delete
        </button>
      </div>
    );
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
        <div className="flex justify-between items-center">
          <div>
            <h1 className="dashboard-title">Run Monitor</h1>
            <p className="dashboard-subtitle">
              Track and monitor task execution runs in real-time
            </p>
          </div>
          <button
            className="btn btn-secondary"
            onClick={fetchRuns}
            disabled={refreshing}
          >
            <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      {/* Runs Overview */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Total Runs</div>
            <div className="stat-icon info">
              <Eye size={20} />
            </div>
          </div>
          <div className="stat-value">{runs.length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Completed</div>
            <div className="stat-icon success">
              <CheckCircle size={20} />
            </div>
          </div>
          <div className="stat-value">{runs.filter(r => r.status === 'DONE').length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Running</div>
            <div className="stat-icon warning">
              <Clock size={20} />
            </div>
          </div>
          <div className="stat-value">{runs.filter(r => r.status === 'RUNNING').length}</div>
        </div>
        <div className="stat-card">
          <div className="stat-header">
            <div className="stat-title">Failed</div>
            <div className="stat-icon error">
              <XCircle size={20} />
            </div>
          </div>
          <div className="stat-value">{runs.filter(r => r.status === 'FAILED').length}</div>
        </div>
      </div>

      {/* Runs List */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Execution Runs</h3>
        </div>
        <div className="card-content">
          {runs.length === 0 ? (
            <div className="text-center py-8">
              <AlertTriangle size={48} className="mx-auto text-gray-400 mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No runs yet</h3>
              <p className="text-gray-500">Create a task and run it to see execution details here</p>
            </div>
          ) : (
            <div className="space-y-4">
              {runs.map((run) => (
                <div key={run.id} className="border border-gray-200 rounded-lg">
                  <div
                    className="p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                    onClick={() => handleRunSelect(run.id)}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2">
                          {getStatusIcon(run.status)}
                          <span className={getStatusBadge(run.status)}>
                            {run.status}
                          </span>
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">
                            Run {run.id.substring(0, 8)}
                          </div>
                          <div className="text-sm text-gray-500">
                            Task: {run.taskId.substring(0, 8)}
                          </div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-sm text-gray-500">
                          Started: {formatDate(run.createdAt)}
                        </div>
                        <div className="text-sm text-gray-500">
                          Duration: {formatDuration(run.createdAt, run.updatedAt)}
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Run Steps Details */}
                  {selectedRun === run.id && (
                    <div className="border-t border-gray-200 bg-gray-50">
                      <div className="p-4">
                        <h4 className="font-medium text-gray-900 mb-3">Execution Steps</h4>
                        {runSteps.length === 0 ? (
                          <p className="text-gray-500">No steps available</p>
                        ) : (
                          <div className="space-y-3">
                            {runSteps.map((step, index) => (
                              <div key={step.id} className="bg-white rounded-lg border border-gray-200">
                                <div
                                  className="p-3 cursor-pointer hover:bg-gray-50"
                                  onClick={() => toggleStepExpanded(step.id)}
                                >
                                  <div className="flex items-center justify-between">
                                    <div className="flex items-center gap-3">
                                      <div className="flex items-center gap-1">
                                        {expandedSteps.has(step.id) ? 
                                          <ChevronDown size={16} /> : 
                                          <ChevronRight size={16} />
                                        }
                                        <span className="text-sm font-medium text-gray-500">
                                          Step {index + 1}
                                        </span>
                                      </div>
                                      <div className="flex items-center gap-2">
                                        {getStatusIcon(step.status)}
                                        <span className="font-medium text-gray-900">
                                          {step.name}
                                        </span>
                                      </div>
                                    </div>
                                    <span className={getStatusBadge(step.status)}>
                                      {step.status}
                                    </span>
                                  </div>
                                </div>

                                {expandedSteps.has(step.id) && (
                                  <div className="px-3 pb-3 border-t border-gray-100">
                                    <div className="mt-3 space-y-3">
                                      {step.description && (
                                        <div>
                                          <div className="text-sm font-medium text-gray-700">Description</div>
                                          <div className="text-sm text-gray-600">{step.description}</div>
                                        </div>
                                      )}
                                      
                                      {step.result && (
                                        <div>
                                          <div className="text-sm font-medium text-gray-700">Result</div>
                                          <div className="text-sm text-gray-600 bg-gray-50 p-2 rounded">
                                            {step.result}
                                          </div>
                                        </div>
                                      )}

                                      {step.errorMessage && (
                                        <div>
                                          <div className="text-sm font-medium text-red-700">Error</div>
                                          <div className="text-sm text-red-600 bg-red-50 p-2 rounded">
                                            {step.errorMessage}
                                          </div>
                                        </div>
                                      )}

                                      {step.artifacts && step.artifacts.length > 0 && (
                                        <div>
                                          <div className="text-sm font-medium text-gray-700 mb-2">
                                            Artifacts ({step.artifacts.length})
                                          </div>
                                          <div className="space-y-2">
                                            {step.artifacts.map((artifact) => (
                                              <div key={artifact.id} className="border border-gray-200 rounded p-3 bg-gray-50">
                                                <div className="flex justify-between items-start mb-2">
                                                  <div>
                                                    <span className="font-medium text-sm text-gray-900">{artifact.name}</span>
                                                    <span className="text-xs text-gray-500 ml-2">
                                                      ({artifact.type}, {artifact.size} bytes)
                                                    </span>
                                                  </div>
                                                </div>
                                                {artifact.content && (
                                                  <div className="mt-2">
                                                    <div className="text-xs text-gray-600 mb-1">Content:</div>
                                                    <div className={`text-xs p-3 rounded border font-mono max-h-60 overflow-y-auto ${getArtifactDisplayClass(artifact.type)}`}>
                                                      {formatArtifactContent(artifact)}
                                                    </div>
                                                    {artifact.content.length > 500 && (
                                                      <button 
                                                        className="text-xs text-blue-600 mt-2 hover:underline px-2 py-1 bg-blue-50 rounded"
                                                        onClick={() => showFullArtifact(artifact)}
                                                      >
                                                        📄 View Full Content
                                                      </button>
                                                    )}
                                                    {getArtifactActions(artifact)}
                                                  </div>
                                                )}
                                                <div className="text-xs text-gray-400 mt-2">
                                                  Created: {formatDate(artifact.createdAt)}
                                                </div>
                                              </div>
                                            ))}
                                          </div>
                                        </div>
                                      )}

                                      <div className="text-xs text-gray-500 pt-2 border-t border-gray-100">
                                        Created: {formatDate(step.createdAt)} | 
                                        Updated: {formatDate(step.updatedAt)}
                                      </div>
                                    </div>
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default RunMonitor;