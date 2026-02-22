import React, { useState, useEffect, useRef } from 'react';
import { Send, Cpu, Brain, Eye, Play, Zap, Clock, CheckCircle, AlertCircle } from 'lucide-react';

interface ExecutionStep {
  id: string;
  name: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  agent: 'planner' | 'executor' | 'critic';
  result?: string;
  artifacts?: { id: string; name: string; type: string; size: number; content?: string }[];
  timestamp: string;
  duration?: number;
}

interface LiveExecution {
  id: string;
  prompt: string;
  status: 'processing' | 'completed' | 'failed';
  steps: ExecutionStep[];
  startTime: string;
}

const Dashboard: React.FC = () => {
  const [prompt, setPrompt] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [currentExecution, setCurrentExecution] = useState<LiveExecution | null>(null);
  const [executionHistory, setExecutionHistory] = useState<LiveExecution[]>([]);
  const [previewModal, setPreviewModal] = useState<{artifact: any, isOpen: boolean}>({artifact: null, isOpen: false});
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [currentExecution]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || isProcessing) return;

    const userPrompt = prompt.trim();
    setPrompt('');
    setIsProcessing(true);

    // Create execution tracking object
    const executionId = crypto.randomUUID();
    const newExecution: LiveExecution = {
      id: executionId,
      prompt: userPrompt,
      status: 'processing',
      steps: [],
      startTime: new Date().toISOString()
    };
    
    setCurrentExecution(newExecution);

    try {
      // Step 1: Create task (secretly)
      const taskResponse = await fetch('http://localhost:8080/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: `AI Task: ${userPrompt.substring(0, 50)}...`,
          description: userPrompt
        }),
      });

      if (!taskResponse.ok) throw new Error('Failed to create task');
      const task = await taskResponse.json();

      // Step 2: Create run
      const runResponse = await fetch('http://localhost:8080/runs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          taskId: task.id,
          status: 'PENDING'
        }),
      });

      if (!runResponse.ok) throw new Error('Failed to create run');
      const run = await runResponse.json();

      // Step 3: Poll for execution updates
      await pollExecutionProgress(run.id, newExecution);

    } catch (error) {
      console.error('Execution failed:', error);
      newExecution.status = 'failed';
      setCurrentExecution({ ...newExecution });
      setExecutionHistory(prev => [...prev, newExecution]);
    } finally {
      setIsProcessing(false);
    }
  };

  const pollExecutionProgress = async (runId: string, execution: LiveExecution) => {
    const maxAttempts = 60; // 5 minutes max
    let attempts = 0;
    
    const poll = async () => {
      try {
        // Get run status
        const runResponse = await fetch(`http://localhost:8080/runs/${runId}`);
        const run = await runResponse.json();

        // Get steps
        const stepsResponse = await fetch(`http://localhost:8080/runs/${runId}/steps`);
        const steps = await stepsResponse.json();

        // Update execution with live data
        const updatedSteps: ExecutionStep[] = steps.map((step: any, index: number) => ({
          id: step.id,
          name: step.name || step.description,
          status: step.status.toLowerCase() as 'pending' | 'running' | 'completed' | 'failed',
          agent: getAgentType(step.name || step.description, index),
          result: step.result,
          artifacts: step.artifacts,
          timestamp: step.updatedAt,
          duration: step.startedAt ? 
            Math.floor((new Date(step.updatedAt).getTime() - new Date(step.startedAt).getTime()) / 1000) : 
            undefined
        }));

        const executionStatus: 'processing' | 'completed' | 'failed' = 
          run.status === 'DONE' ? 'completed' : 
          run.status === 'FAILED' ? 'failed' : 'processing';

        const updatedExecution: LiveExecution = {
          ...execution,
          steps: updatedSteps,
          status: executionStatus
        };

        setCurrentExecution(updatedExecution);

        // Check if completed
        if (run.status === 'DONE' || run.status === 'FAILED') {
          setExecutionHistory(prev => [...prev, updatedExecution]);
          return;
        }

        // Continue polling
        if (attempts < maxAttempts) {
          attempts++;
          setTimeout(poll, 2000); // Poll every 2 seconds
        } else {
          const failedExecution: LiveExecution = { ...updatedExecution, status: 'failed' };
          setCurrentExecution(failedExecution);
          setExecutionHistory(prev => [...prev, failedExecution]);
        }

      } catch (error) {
        console.error('Polling error:', error);
        execution.status = 'failed';
        setCurrentExecution(execution);
        setExecutionHistory(prev => [...prev, execution]);
      }
    };

    poll();
  };

  const getAgentType = (stepName: string, index: number): 'planner' | 'executor' | 'critic' => {
    const name = stepName.toLowerCase();
    if (name.includes('planning') || name.includes('plan')) return 'planner';
    if (name.includes('critique') || name.includes('evaluation')) return 'critic';
    return 'executor';
  };

  const getAgentIcon = (agent: 'planner' | 'executor' | 'critic') => {
    switch (agent) {
      case 'planner': return <Brain size={16} className="text-purple-500" />;
      case 'executor': return <Cpu size={16} className="text-blue-500" />;
      case 'critic': return <Eye size={16} className="text-green-500" />;
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'pending': return <Clock size={14} className="text-yellow-500" />;
      case 'running': return <Zap size={14} className="text-blue-500 animate-pulse" />;
      case 'completed': case 'done': return <CheckCircle size={14} className="text-green-500" />;
      case 'failed': return <AlertCircle size={14} className="text-red-500" />;
      default: return <Clock size={14} className="text-gray-500" />;
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString();
  };

  const getArtifactStyleClass = (name: string, type: string) => {
    if (type.includes('image')) return 'bg-yellow-50 border-yellow-200 text-yellow-800';
    if (type.includes('text')) return 'bg-green-50 border-green-200 text-green-800';
    if (type.includes('application')) return 'bg-blue-50 border-blue-200 text-blue-800';
    return 'bg-gray-50 border-gray-200 text-gray-800';
  };

  const getArtifactIcon = (name: string) => {
    if (name.toLowerCase().includes('image')) return <img src="/icons/image-icon.png" alt="Image" className="inline-block mr-1" />;
    if (name.toLowerCase().includes('text')) return <img src="/icons/text-icon.png" alt="Text" className="inline-block mr-1" />;
    if (name.toLowerCase().includes('pdf')) return <img src="/icons/pdf-icon.png" alt="PDF" className="inline-block mr-1" />;
    return <img src="/icons/file-icon.png" alt="File" className="inline-block mr-1" />;
  };

  const getArtifactPreview = (artifact: any) => {
    if (artifact.type.includes('image')) return <img src={artifact.content} alt={artifact.name} className="max-w-full h-auto" />;
    if (artifact.type.includes('text')) return artifact.content.length > 100 ? `${artifact.content.substring(0, 100)}...` : artifact.content;
    return 'Preview not available';
  };

  const previewArtifact = (artifact: any) => {
    setPreviewModal({artifact, isOpen: true});
  };

  const downloadArtifact = (artifact: any) => {
    const blob = new Blob([artifact.content], { type: getContentType(artifact.type) });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = artifact.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const getContentType = (artifactType: string) => {
    switch (artifactType.toLowerCase()) {
      case 'json': return 'application/json';
      case 'markdown': 
      case 'md': return 'text/markdown';
      case 'text':
      case 'txt': return 'text/plain';
      default: return 'text/plain';
    }
  };

  const closePreviewModal = () => {
    setPreviewModal({artifact: null, isOpen: false});
  };

  const formatPreviewContent = (artifact: any) => {
    if (!artifact || !artifact.content) return '';
    
    switch (artifact.type.toLowerCase()) {
      case 'json':
        try {
          return JSON.stringify(JSON.parse(artifact.content), null, 2);
        } catch {
          return artifact.content;
        }
      case 'markdown':
      case 'md':
        return artifact.content;
      default:
        return artifact.content;
    }
  };

  const formatFileSize = (size: number) => {
    if (size < 1024) return `${size} bytes`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
    return `${(size / (1024 * 1024)).toFixed(2)} MB`;
  };

  const getArtifactFileIcon = (name: string) => {
    if (name.toLowerCase().includes('image')) return '🖼️';
    if (name.toLowerCase().includes('text')) return '📄';
    if (name.toLowerCase().includes('pdf')) return '📄';
    return '📁';
  };

  return (
    <div className="dashboard" style={{ height: 'calc(100vh - 4rem)', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <div className="dashboard-header" style={{ paddingBottom: '1rem' }}>
        <h1 className="dashboard-title" style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>
          🧠 Agentic AI Prompt Engine
        </h1>
        <p className="dashboard-subtitle">
          Enter your prompt below and watch the AI agents work in real-time
        </p>
      </div>

      {/* Execution Display */}
      <div className="card" style={{ flex: 1, display: 'flex', flexDirection: 'column', marginBottom: '1rem' }}>
        <div className="card-header">
          <h3 className="card-title">Live Execution Monitor</h3>
        </div>
        <div className="card-content" style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <div style={{ flex: 1, overflowY: 'auto', marginBottom: '1rem' }}>
            
            {/* Current Execution */}
            {currentExecution && (
              <div className="mb-6">
                <div className="flex items-center gap-2 mb-3">
                  <div className="w-3 h-3 bg-blue-500 rounded-full animate-pulse"></div>
                  <span className="font-semibold text-gray-900">Processing:</span>
                  <span className="text-gray-700">{currentExecution.prompt}</span>
                </div>
                
                {/* Live Steps */}
                <div className="space-y-2 pl-4 border-l-2 border-blue-200">
                  {currentExecution.steps.map((step, index) => (
                    <div key={step.id} className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-2 min-w-0 flex-1">
                        {getAgentIcon(step.agent)}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-medium text-sm text-gray-900">
                              Step {index + 1}: {step.name}
                            </span>
                            {getStatusIcon(step.status)}
                          </div>
                          <div className="text-xs text-gray-500 mt-1">
                            {formatTimestamp(step.timestamp)}
                            {step.duration && ` • ${step.duration}s`}
                          </div>
                          {step.result && step.status === 'completed' && (
                            <div className="mt-2 p-2 bg-white rounded border text-xs text-gray-700">
                              {step.result.length > 200 ? 
                                `${step.result.substring(0, 200)}...` : 
                                step.result
                              }
                            </div>
                          )}
                          {step.artifacts && step.artifacts.length > 0 && (
                            <div className="mt-2">
                              <div className="text-xs text-gray-600 mb-1">Artifacts ({step.artifacts.length}):</div>
                              <div className="space-y-1">
                                {step.artifacts.map((artifact: any) => (
                                  <div key={artifact.id} className="flex items-center justify-between p-2 bg-white rounded border border-gray-200 hover:border-gray-300 transition-colors">
                                    <div className="flex items-center gap-2 min-w-0 flex-1">
                                      <div className="text-xs">
                                        {getArtifactFileIcon(artifact.name)}
                                      </div>
                                      <div className="min-w-0 flex-1">
                                        <div className="font-medium text-xs text-gray-900 truncate">
                                          {artifact.name}
                                        </div>
                                        <div className="text-xs text-gray-500">
                                          {artifact.type} • {formatFileSize(artifact.size)}
                                        </div>
                                      </div>
                                    </div>
                                    <div className="flex items-center gap-1 ml-2">
                                      <button 
                                        className="text-xs px-2 py-1 bg-blue-50 text-blue-700 hover:bg-blue-100 rounded border border-blue-200 transition-colors"
                                        onClick={() => previewArtifact(artifact)}
                                        title="Preview content"
                                      >
                                        👁️ Preview
                                      </button>
                                      <button 
                                        className="text-xs px-2 py-1 bg-green-50 text-green-700 hover:bg-green-100 rounded border border-green-200 transition-colors"
                                        onClick={() => downloadArtifact(artifact)}
                                        title="Download file"
                                      >
                                        ⬇️ Download
                                      </button>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                      <div className={`px-2 py-1 text-xs rounded-full ${
                        step.status === 'completed' ? 'bg-green-100 text-green-800' :
                        step.status === 'running' ? 'bg-blue-100 text-blue-800' :
                        step.status === 'failed' ? 'bg-red-100 text-red-800' :
                        'bg-yellow-100 text-yellow-800'
                      }`}>
                        {step.status}
                      </div>
                    </div>
                  ))}
                  
                  {/* Processing indicator */}
                  {isProcessing && currentExecution.steps.length === 0 && (
                    <div className="flex items-center gap-2 p-3 bg-blue-50 rounded-lg">
                      <div className="animate-spin">
                        <Cpu size={16} className="text-blue-500" />
                      </div>
                      <span className="text-sm text-blue-700">Initializing AI agents...</span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Execution History */}
            {executionHistory.map((execution) => (
              <div key={execution.id} className="mb-4 opacity-75">
                <div className="flex items-center gap-2 mb-2">
                  <div className={`w-3 h-3 rounded-full ${
                    execution.status === 'completed' ? 'bg-green-500' : 
                    execution.status === 'failed' ? 'bg-red-500' : 'bg-gray-500'
                  }`}></div>
                  <span className="font-medium text-gray-700">
                    {execution.prompt}
                  </span>
                  <span className="text-xs text-gray-500">
                    ({execution.steps.length} steps)
                  </span>
                </div>
              </div>
            ))}

            <div ref={messagesEndRef} />
          </div>

          {/* Prompt Input */}
          <form onSubmit={handleSubmit} className="flex gap-2">
            <input
              type="text"
              className="form-input flex-1"
              placeholder="Enter your prompt here... (e.g., 'Create a marketing plan for a new AI product')"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              disabled={isProcessing}
              style={{ fontSize: '0.9rem' }}
            />
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isProcessing || !prompt.trim()}
            >
              {isProcessing ? (
                <div className="animate-spin"><Cpu size={16} /></div>
              ) : (
                <Send size={16} />
              )}
            </button>
          </form>
        </div>
      </div>

      {/* Artifact Preview Modal */}
      {previewModal.isOpen && previewModal.artifact && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={closePreviewModal}>
          <div className="bg-white rounded-lg shadow-xl max-w-4xl max-h-[80vh] w-full mx-4" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between p-4 border-b border-gray-200">
              <div className="flex items-center gap-3">
                <span className="text-lg">{getArtifactFileIcon(previewModal.artifact.name)}</span>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">{previewModal.artifact.name}</h3>
                  <p className="text-sm text-gray-500">
                    {previewModal.artifact.type} • {formatFileSize(previewModal.artifact.size)}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button 
                  className="px-3 py-2 bg-green-50 text-green-700 hover:bg-green-100 rounded border border-green-200 transition-colors text-sm"
                  onClick={() => downloadArtifact(previewModal.artifact)}
                >
                  ⬇️ Download
                </button>
                <button 
                  className="px-3 py-2 bg-gray-50 text-gray-700 hover:bg-gray-100 rounded border border-gray-200 transition-colors text-sm"
                  onClick={closePreviewModal}
                >
                  ✕ Close
                </button>
              </div>
            </div>
            <div className="p-4 max-h-96 overflow-y-auto">
              <pre className="whitespace-pre-wrap text-sm font-mono bg-gray-50 p-4 rounded border">
                {formatPreviewContent(previewModal.artifact)}
              </pre>
            </div>
          </div>
        </div>
      )}

      {/* Agent Status Bar */}
      <div className="grid grid-cols-3 gap-4">
        <div className="card">
          <div className="card-content p-3">
            <div className="flex items-center gap-2">
              <Brain size={20} className="text-purple-500" />
              <div>
                <div className="font-medium text-sm">Planner Agent</div>
                <div className="text-xs text-gray-500">Creates execution strategy</div>
              </div>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="card-content p-3">
            <div className="flex items-center gap-2">
              <Cpu size={20} className="text-blue-500" />
              <div>
                <div className="font-medium text-sm">Executor Agent</div>
                <div className="text-xs text-gray-500">Implements the plan</div>
              </div>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="card-content p-3">
            <div className="flex items-center gap-2">
              <Eye size={20} className="text-green-500" />
              <div>
                <div className="font-medium text-sm">Critic Agent</div>
                <div className="text-xs text-gray-500">Reviews and validates</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;