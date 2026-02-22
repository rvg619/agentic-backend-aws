import React, { useState, useEffect } from 'react';
import { 
  Calendar, 
  Clock, 
  Download, 
  FileText, 
  Code, 
  GitBranch,
  Search,
  Filter,
  CheckCircle,
  AlertCircle,
  Sparkles,
  BarChart3,
  TrendingUp,
  Activity
} from 'lucide-react';
import './ExecutionHistory.css';

interface Task {
  id: string;
  title: string;
  description: string;
  createdAt: string;
}

interface Run {
  id: string;
  taskId: string;
  status: 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED';
  createdAt: string;
  completedAt?: string;
  errorMessage?: string;
}

interface Step {
  id: string;
  runId: string;
  name: string;
  status: 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED';
  result?: string;
  stepOrder: number;
}

interface Artifact {
  id: string;
  stepId: string;
  name: string;
  type: 'TEXT' | 'JSON' | 'CODE';
  content: string;
  size: number;
}

interface RunWithDetails extends Run {
  task: Task;
  steps: Step[];
  artifacts: Artifact[];
}

const ExecutionHistory: React.FC = () => {
  const [runs, setRuns] = useState<RunWithDetails[]>([]);
  const [filteredRuns, setFilteredRuns] = useState<RunWithDetails[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'DONE' | 'FAILED' | 'RUNNING'>('ALL');
  const [loading, setLoading] = useState(true);
  const [selectedRun, setSelectedRun] = useState<RunWithDetails | null>(null);
  const [stats, setStats] = useState({
    totalRuns: 0,
    successRate: 0,
    totalArtifacts: 0,
    avgExecutionTime: 0
  });

  useEffect(() => {
    fetchRunsHistory();
  }, []);

  useEffect(() => {
    filterRuns();
  }, [runs, searchQuery, statusFilter]);

  const fetchRunsHistory = async () => {
    try {
      setLoading(true);
      const response = await fetch('http://localhost:8080/runs');
      
      // Check if response is ok before trying to parse JSON
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const runsData: Run[] = await response.json();
      
      // Fetch detailed information for each run with error handling
      const detailedRuns = await Promise.all(
        runsData.map(async (run) => {
          try {
            const [taskRes, stepsRes, artifactsRes] = await Promise.all([
              fetch(`http://localhost:8080/tasks/${run.taskId}`),
              fetch(`http://localhost:8080/runs/${run.id}/steps`),
              fetch(`http://localhost:8080/runs/${run.id}/artifacts`)
            ]);

            // Initialize with safe defaults
            let task = { id: run.taskId, title: 'Unknown Task', description: '', createdAt: run.createdAt };
            let steps: Step[] = [];
            let artifacts: Artifact[] = [];

            // Parse responses with null checks
            if (taskRes.ok) {
              const taskData = await taskRes.json();
              task = {
                ...taskData,
                description: taskData.description || '' // Ensure description is never null
              };
            }

            if (stepsRes.ok) {
              const stepsData = await stepsRes.json();
              steps = Array.isArray(stepsData) ? stepsData : [];
            }

            if (artifactsRes.ok) {
              const artifactsData = await artifactsRes.json();
              artifacts = Array.isArray(artifactsData) ? artifactsData : [];
            }

            return { ...run, task, steps, artifacts };
          } catch (error) {
            console.error(`Error fetching details for run ${run.id}:`, error);
            // Return run with safe default data on error
            return { 
              ...run, 
              task: { id: run.taskId, title: 'Error Loading Task', description: '', createdAt: run.createdAt }, 
              steps: [], 
              artifacts: [] 
            };
          }
        })
      );

      setRuns(detailedRuns);
      calculateStats(detailedRuns);
    } catch (error) {
      console.error('Error fetching runs history:', error);
      // Set empty state on error
      setRuns([]);
      setStats({ totalRuns: 0, successRate: 0, totalArtifacts: 0, avgExecutionTime: 0 });
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = (runsData: RunWithDetails[]) => {
    const totalRuns = runsData.length;
    const successfulRuns = runsData.filter(run => run.status === 'DONE').length;
    const totalArtifacts = runsData.reduce((sum, run) => sum + run.artifacts.length, 0);
    
    const completedRuns = runsData.filter(run => run.completedAt);
    const avgExecutionTime = completedRuns.length > 0 
      ? completedRuns.reduce((sum, run) => {
          const duration = new Date(run.completedAt!).getTime() - new Date(run.createdAt).getTime();
          return sum + duration;
        }, 0) / completedRuns.length / 1000 // Convert to seconds
      : 0;

    setStats({
      totalRuns,
      successRate: totalRuns > 0 ? (successfulRuns / totalRuns) * 100 : 0,
      totalArtifacts,
      avgExecutionTime
    });
  };

  const filterRuns = () => {
    let filtered = runs;

    if (searchQuery) {
      filtered = filtered.filter(run => 
        run.task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        run.task.description.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    if (statusFilter !== 'ALL') {
      filtered = filtered.filter(run => run.status === statusFilter);
    }

    setFilteredRuns(filtered);
  };

  const downloadArtifact = (artifact: Artifact) => {
    const blob = new Blob([artifact.content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = artifact.name;
    a.click();
    URL.revokeObjectURL(url);
  };

  const downloadAllArtifacts = (run: RunWithDetails) => {
    run.artifacts.forEach(artifact => {
      setTimeout(() => downloadArtifact(artifact), 100);
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DONE': return 'success';
      case 'FAILED': return 'error';
      case 'RUNNING': return 'warning';
      default: return 'info';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'DONE': return <CheckCircle className="status-icon success" />;
      case 'FAILED': return <AlertCircle className="status-icon error" />;
      case 'RUNNING': return <Sparkles className="status-icon warning spinning" />;
      default: return <Clock className="status-icon info" />;
    }
  };

  const getArtifactIcon = (type: string) => {
    switch (type) {
      case 'CODE': return <Code size={16} />;
      case 'JSON': return <GitBranch size={16} />;
      default: return <FileText size={16} />;
    }
  };

  const formatDuration = (startTime: string, endTime?: string) => {
    if (!endTime) return 'N/A';
    const duration = new Date(endTime).getTime() - new Date(startTime).getTime();
    const seconds = Math.floor(duration / 1000);
    const minutes = Math.floor(seconds / 60);
    return minutes > 0 ? `${minutes}m ${seconds % 60}s` : `${seconds}s`;
  };

  const formatExecutionTime = (seconds: number) => {
    if (seconds < 60) return `${Math.round(seconds)}s`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.round(seconds % 60);
    return `${minutes}m ${remainingSeconds}s`;
  };

  return (
    <div className="execution-history">
      <div className="history-header">
        <div className="header-content">
          <h1 className="history-title">
            <Activity className="title-icon" />
            Execution History
          </h1>
          <p className="history-subtitle">
            Comprehensive analytics and artifacts from past orchestration runs
          </p>
        </div>
      </div>

      {/* Analytics Dashboard */}
      <div className="analytics-section">
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon-wrapper success">
              <BarChart3 size={24} />
            </div>
            <div className="stat-content">
              <div className="stat-value">{stats.totalRuns}</div>
              <div className="stat-label">Total Runs</div>
            </div>
          </div>
          
          <div className="stat-card">
            <div className="stat-icon-wrapper info">
              <TrendingUp size={24} />
            </div>
            <div className="stat-content">
              <div className="stat-value">{Math.round(stats.successRate)}%</div>
              <div className="stat-label">Success Rate</div>
            </div>
          </div>
          
          <div className="stat-card">
            <div className="stat-icon-wrapper warning">
              <FileText size={24} />
            </div>
            <div className="stat-content">
              <div className="stat-value">{stats.totalArtifacts}</div>
              <div className="stat-label">Artifacts Generated</div>
            </div>
          </div>
          
          <div className="stat-card">
            <div className="stat-icon-wrapper error">
              <Clock size={24} />
            </div>
            <div className="stat-content">
              <div className="stat-value">{formatExecutionTime(stats.avgExecutionTime)}</div>
              <div className="stat-label">Avg Execution Time</div>
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="filters-section">
        <div className="search-wrapper">
          <Search className="search-icon" />
          <input
            type="text"
            placeholder="Search runs by task title or description..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>
        
        <div className="filter-wrapper">
          <Filter className="filter-icon" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as any)}
            className="filter-select"
          >
            <option value="ALL">All Status</option>
            <option value="DONE">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="RUNNING">Running</option>
          </select>
        </div>
      </div>

      {/* Runs List */}
      <div className="runs-section">
        {loading ? (
          <div className="loading-state">
            <div className="loading-spinner" />
            <p>Loading execution history...</p>
          </div>
        ) : filteredRuns.length === 0 ? (
          <div className="empty-state">
            <Activity size={48} />
            <h3>No runs found</h3>
            <p>Try adjusting your search or filters, or create a new run in the Prompt Engine.</p>
          </div>
        ) : (
          <div className="runs-grid">
            {filteredRuns.map((run) => (
              <div
                key={run.id}
                className={`run-card ${selectedRun?.id === run.id ? 'selected' : ''}`}
                onClick={() => setSelectedRun(selectedRun?.id === run.id ? null : run)}
              >
                <div className="run-header">
                  <div className="run-title-section">
                    <h3 className="run-title">{run.task.title}</h3>
                    <div className="run-meta">
                      <Calendar size={14} />
                      <span>{new Date(run.createdAt).toLocaleDateString()}</span>
                      <Clock size={14} />
                      <span>{formatDuration(run.createdAt, run.completedAt)}</span>
                    </div>
                  </div>
                  <div className="run-status">
                    {getStatusIcon(run.status)}
                    <span className={`status-label ${getStatusColor(run.status)}`}>
                      {run.status}
                    </span>
                  </div>
                </div>

                <div className="run-description">
                  {run.task.description.length > 150 
                    ? `${run.task.description.slice(0, 150)}...`
                    : run.task.description
                  }
                </div>

                <div className="run-summary">
                  <div className="summary-item">
                    <span className="summary-label">Steps:</span>
                    <span className="summary-value">{run.steps.length}</span>
                  </div>
                  <div className="summary-item">
                    <span className="summary-label">Artifacts:</span>
                    <span className="summary-value">{run.artifacts.length}</span>
                  </div>
                  {run.artifacts.length > 0 && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        downloadAllArtifacts(run);
                      }}
                      className="download-all-btn"
                      title="Download all artifacts"
                    >
                      <Download size={14} />
                    </button>
                  )}
                </div>

                {selectedRun?.id === run.id && (
                  <div className="run-details">
                    <div className="details-tabs">
                      <div className="tab-content">
                        <div className="steps-section">
                          <h4>Execution Steps</h4>
                          <div className="steps-timeline">
                            {run.steps.map((step, index) => (
                              <div key={step.id} className="timeline-item">
                                <div className="timeline-marker">
                                  <span className="step-number">{index + 1}</span>
                                </div>
                                <div className="timeline-content">
                                  <div className="step-header">
                                    <span className="step-name">{step.name}</span>
                                    {getStatusIcon(step.status)}
                                  </div>
                                  {step.result && (
                                    <div className="step-result">
                                      {step.result.slice(0, 200)}
                                      {step.result.length > 200 && '...'}
                                    </div>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>

                        {run.artifacts.length > 0 && (
                          <div className="artifacts-section">
                            <h4>Generated Artifacts</h4>
                            <div className="artifacts-list">
                              {run.artifacts.map((artifact) => (
                                <div key={artifact.id} className="artifact-item">
                                  <div className="artifact-icon">
                                    {getArtifactIcon(artifact.type)}
                                  </div>
                                  <div className="artifact-info">
                                    <div className="artifact-name">{artifact.name}</div>
                                    <div className="artifact-meta">
                                      <span className="artifact-type">{artifact.type}</span>
                                      <span className="artifact-size">{artifact.size} bytes</span>
                                    </div>
                                  </div>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      downloadArtifact(artifact);
                                    }}
                                    className="artifact-download-btn"
                                  >
                                    <Download size={14} />
                                  </button>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
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
  );
};

export default ExecutionHistory;